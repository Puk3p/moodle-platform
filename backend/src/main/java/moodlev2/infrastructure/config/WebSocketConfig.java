package moodlev2.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import moodlev2.common.util.TokenHashUtil;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.security.StompAccessInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private static final String PRINCIPAL_ATTRIBUTE = "moodlev2.ws.email";

    private final TokenServicePort tokenService;
    private final UserSessionRepository userSessionRepository;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(
            TokenServicePort tokenService,
            UserSessionRepository userSessionRepository,
            ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.userSessionRepository = userSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
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
     * Refuses the upgrade outright unless the token is valid and its session has not been revoked.
     * Previously a bad token still produced an anonymous socket, which could then publish frames.
     */
    private final class TokenHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            String email = authenticate(request);
            if (email == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(PRINCIPAL_ATTRIBUTE, email);
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {}
    }

    /**
     * Resolves the user from the {@code access_token} query parameter (browsers cannot set headers
     * on a WebSocket upgrade). Signature and expiry are verified via {@link TokenServicePort}, and
     * the session row must still exist, matching what {@code JwtAuthenticationFilter} requires of
     * REST calls — so logging out, or revoking a session in Settings, also closes off the socket.
     */
    private String authenticate(ServerHttpRequest request) {
        String raw =
                UriComponentsBuilder.fromUri(request.getURI())
                        .build()
                        .getQueryParams()
                        .getFirst("access_token");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String token = URLDecoder.decode(raw, StandardCharsets.UTF_8);

        try {
            if (!tokenService.isValid(token)
                    || !userSessionRepository.existsByTokenSignature(TokenHashUtil.sha256(token))) {
                return null;
            }
            return tokenService.parse(token).email();
        } catch (RuntimeException e) {
            log.warn("Rejected WebSocket handshake: invalid token");
            return null;
        }
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
