package moodlev2.infrastructure.security;

import java.util.Set;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;

/**
 * Filters every STOMP frame a browser sends. The socket is receive-only.
 *
 * <p>Without this the simple broker relays any client SEND: a frame addressed to {@code
 * /user/<email>/queue/private} is delivered straight to that user, skipping the chat controller —
 * no role check, no quiz lock, no persistence, and a sender field the client writes itself. A
 * SUBSCRIBE to {@code /topic/**} or a raw {@code /queue/**} name is likewise refused.
 *
 * <p>A rejected frame makes Spring answer with an ERROR frame and close the connection.
 */
public class StompAccessInterceptor implements ChannelInterceptor {

    /**
     * Per-user queues, resolved from the session's authenticated principal, so a client can only
     * ever receive its own.
     */
    static final Set<String> SUBSCRIBABLE =
            Set.of("/user/queue/private", "/user/queue/chat-status");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (command == StompCommand.SEND) {
            // All writes go through the REST API, where authorization and the quiz lock apply.
            throw new AccessDeniedException("Publishing over the socket is disabled");
        }
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            requireUser(accessor);
        }
        if (command == StompCommand.SUBSCRIBE) {
            requireUser(accessor);
            if (!SUBSCRIBABLE.contains(accessor.getDestination())) {
                throw new AccessDeniedException("Subscription not permitted");
            }
        }
        // UNSUBSCRIBE, DISCONNECT, ACK, NACK and transactions carry nothing to protect.
        return message;
    }

    private static void requireUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("Not authenticated");
        }
    }
}
