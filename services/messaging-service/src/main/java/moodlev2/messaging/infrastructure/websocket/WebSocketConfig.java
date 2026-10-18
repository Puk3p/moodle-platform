package moodlev2.messaging.infrastructure.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String PRINCIPAL_ATTRIBUTE = "moodlev2.ws.email";

    private final JwtDecoder jwtDecoder;
    private final WebSocketSessionRegistry socketRegistry;
    private final ObjectMapper objectMapper;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            JwtDecoder jwtDecoder,
            WebSocketSessionRegistry socketRegistry,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.jwtDecoder = jwtDecoder;
        this.socketRegistry = socketRegistry;
        this.objectMapper = objectMapper;
        this.allowedOrigins =
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(o -> !o.isEmpty())
                        .toArray(String[]::new);
    }

    /**
     * Only our own front-end may open a socket. Browsers attach cookies to cross-site WebSocket
     * handshakes, so with a wildcard any website could open a live channel as its visitor
     * (cross-site WebSocket hijacking). Same-origin handshakes are always allowed by Spring.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .addInterceptors(new TokenHandshakeInterceptor())
                .setHandshakeHandler(
                        new DefaultHandshakeHandler() {
                            @Override
                            protected Principal determineUser(
                                    ServerHttpRequest request,
                                    WebSocketHandler wsHandler,
                                    Map<String, Object> attributes) {
                                Object email = attributes.get(PRINCIPAL_ATTRIBUTE);
                                return email instanceof String name ? () -> name : null;
                            }
                        })
                .withSockJS();
    }

    /**
     * Authenticates the upgrade from the user token the gateway puts on it (the browser's own
     * session cookie never reaches this service). Same token, same checks as the REST API; the
     * session id ({@code sid}) is kept so the socket can be closed when that login ends.
     */
    private final class TokenHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            Optional<Jwt> token = bearer(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            String email = token.map(t -> t.getClaimAsString("email")).orElse(null);
            if (email == null || !"user".equals(token.get().getClaimAsString("scope"))) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(PRINCIPAL_ATTRIBUTE, email);
            String sid = token.get().getClaimAsString("sid");
            if (sid != null) {
                attributes.put(WebSocketSessionRegistry.SESSION_HASH, sid);
            }
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {}

        private Optional<Jwt> bearer(String authorization) {
            if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return Optional.empty();
            }
            try {
                return Optional.of(jwtDecoder.decode(authorization.substring(7).trim()));
            } catch (JwtException e) {
                return Optional.empty();
            }
        }
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(socketRegistry);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new StompAccessInterceptor());
    }

    /**
     * Serialize pushes with the application's ObjectMapper, so a payload over the socket is
     * byte-for-byte what the same DTO looks like over REST (ISO-8601 instants, not arrays).
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        messageConverters.add(converter);
        return true;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
