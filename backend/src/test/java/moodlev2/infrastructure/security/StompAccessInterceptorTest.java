package moodlev2.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

class StompAccessInterceptorTest {

    private final StompAccessInterceptor interceptor = new StompAccessInterceptor();
    private final MessageChannel channel = mock(MessageChannel.class);
    private final Principal student = () -> "student@test.com";

    private static Message<byte[]> frame(StompCommand command, String destination, Principal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void authenticatedConnectPasses() {
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, student);
        assertThat(interceptor.preSend(connect, channel)).isSameAs(connect);
    }

    @Test
    void anonymousConnectIsRefused() {
        assertThatThrownBy(
                        () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void ownQueuesMayBeSubscribed() {
        for (String dest : StompAccessInterceptor.SUBSCRIBABLE) {
            Message<byte[]> sub = frame(StompCommand.SUBSCRIBE, dest, student);
            assertThat(interceptor.preSend(sub, channel)).isSameAs(sub);
        }
    }

    @Test
    void broadcastTopicsAndRawQueuesCannotBeSubscribed() {
        for (String dest :
                new String[] {
                    "/topic/anything",
                    "/queue/private",
                    "/queue/private-user3kq2mx1a",
                    "/user/teacher@test.com/queue/private"
                }) {
            assertThatThrownBy(
                            () ->
                                    interceptor.preSend(
                                            frame(StompCommand.SUBSCRIBE, dest, student), channel))
                    .as(dest)
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void anonymousSubscribeIsRefusedEvenToAnAllowedQueue() {
        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        frame(StompCommand.SUBSCRIBE, "/user/queue/private", null),
                                        channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void everySendIsRefusedSoTheRestRulesCannotBeBypassed() {
        for (String dest :
                new String[] {
                    "/app/chat.sendPrivate",
                    "/user/teacher@test.com/queue/private",
                    "/queue/private-user3kq2mx1a",
                    "/topic/anything"
                }) {
            assertThatThrownBy(
                            () ->
                                    interceptor.preSend(
                                            frame(StompCommand.SEND, dest, student), channel))
                    .as(dest)
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Test
    void disconnectNeedsNoChecks() {
        Message<byte[]> bye = frame(StompCommand.DISCONNECT, null, null);
        assertThat(interceptor.preSend(bye, channel)).isSameAs(bye);
    }
}
