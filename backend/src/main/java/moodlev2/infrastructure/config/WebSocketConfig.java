package moodlev2.infrastructure.config;

import java.security.Principal;
import java.util.Map;
import moodlev2.domain.auth.ports.TokenServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final TokenServicePort tokenService;

    public WebSocketConfig(TokenServicePort tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(
                        new DefaultHandshakeHandler() {
                            @Override
                            protected Principal determineUser(
                                    ServerHttpRequest request,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
                                String email = authenticate(request);
                                if (email == null) {
                                    return null;
                                }
                                return () -> email;
                            }
                        })
                .withSockJS();
    }

    /**
     * Resolves the authenticated user from the {@code access_token} query parameter. The token's
     * signature and expiry are cryptographically verified via {@link TokenServicePort}; an
     * unsigned, tampered or expired token yields {@code null} so the handshake is rejected.
     */
    private String authenticate(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query == null || !query.contains("access_token=")) {
            return null;
        }

        String token = query.split("access_token=")[1];
        int amp = token.indexOf('&');
        if (amp >= 0) {
            token = token.substring(0, amp);
        }

        try {
            if (!tokenService.isValid(token)) {
                return null;
            }
            return tokenService.parse(token).email();
        } catch (RuntimeException e) {
            log.warn("Rejected WebSocket handshake: invalid token");
            return null;
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
