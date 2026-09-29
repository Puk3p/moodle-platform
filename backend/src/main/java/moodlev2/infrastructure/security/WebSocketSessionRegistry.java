package moodlev2.infrastructure.security;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import moodlev2.application.auth.SessionService.SessionsRevokedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Tracks open WebSocket connections by the login session that opened them, and closes them when
 * that session is revoked (logout, "sign out other devices", password change or reset, theft
 * detection). Without this, a revoked session could keep receiving live messages until the socket
 * happened to drop.
 */
@Component
public class WebSocketSessionRegistry implements WebSocketHandlerDecoratorFactory {

    /** Handshake attribute holding the login session's token hash. */
    public static final String SESSION_HASH = "moodlev2.ws.sessionHash";

    private final Map<String, Set<WebSocketSession>> byLoginSession = new ConcurrentHashMap<>();

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                if (session.getAttributes().get(SESSION_HASH) instanceof String hash) {
                    byLoginSession
                            .computeIfAbsent(hash, k -> ConcurrentHashMap.newKeySet())
                            .add(session);
                }
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status)
                    throws Exception {
                if (session.getAttributes().get(SESSION_HASH) instanceof String hash) {
                    Set<WebSocketSession> open = byLoginSession.get(hash);
                    if (open != null) {
                        open.remove(session);
                        if (open.isEmpty()) {
                            byLoginSession.remove(hash);
                        }
                    }
                }
                super.afterConnectionClosed(session, status);
            }
        };
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onSessionsRevoked(SessionsRevokedEvent event) {
        for (String hash : event.tokenHashes()) {
            Set<WebSocketSession> open = byLoginSession.remove(hash);
            if (open == null) {
                continue;
            }
            for (WebSocketSession ws : open) {
                try {
                    ws.close(CloseStatus.POLICY_VIOLATION.withReason("Session ended"));
                } catch (IOException e) {
                    // Already gone.
                }
            }
        }
    }
}
