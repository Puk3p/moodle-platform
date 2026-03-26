package moodlev2.infrastructure.config;

import java.security.Principal;
import java.util.Map;
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
                                String query = request.getURI().getQuery();

                                if (query != null && query.contains("access_token=")) {
                                    String token = query.split("access_token=")[1];

                                    if (token.contains("&")) {
                                        token = token.split("&")[0];
                                    }

                                    String email = extractEmailFromToken(token);

                                    if (email != null) {
                                        final String finalEmail = email;
                                        return new Principal() {
                                            @Override
                                            public String getName() {
                                                return finalEmail;
                                            }
                                        };
                                    }
                                }
                                return null;
                            }
                        })
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        // Activează broker-ul pentru public și private (/user)
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    private String extractEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(java.util.Base64.getDecoder().decode(parts[1]));

                if (payload.contains("\"sub\":\"")) {
                    return payload.split("\"sub\":\"")[1].split("\"")[0];
                } else if (payload.contains("\"email\":\"")) {
                    return payload.split("\"email\":\"")[1].split("\"")[0];
                }
            }
        } catch (Exception e) {
            log.warn("WebSocket token parsing error: {}", e.getMessage());
        }
        return null;
    }
}
