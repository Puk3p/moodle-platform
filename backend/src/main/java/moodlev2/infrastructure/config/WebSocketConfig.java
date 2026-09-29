package moodlev2.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import moodlev2.application.auth.SessionService;
import moodlev2.infrastructure.security.SessionCookies;
import moodlev2.infrastructure.security.StompAccessInterceptor;
import moodlev2.infrastructure.security.WebSocketSessionRegistry;
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

    private final SessionService sessionService;
    private final SessionCookies cookies;
    private final WebSocketSessionRegistry socketRegistry;
    private final ObjectMapper objectMapper;
    private final String[] allowedOrigins;

    public WebSocketConfig(
            SessionService sessionService,
            SessionCookies cookies,
            WebSocketSessionRegistry socketRegistry,
            ObjectMapper objectMapper,
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.sessionService = sessionService;
        this.cookies = cookies;
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
                .addInterceptors(new CookieHandshakeInterceptor())
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
     * Authenticates the upgrade from the HttpOnly session cookie, with the same checks as every
     * HTTP request (expiry, idle, same browser, active account). No token in the URL any more,
     * where proxies and access logs would record it.
     */
    private final class CookieHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Map<String, Object> attributes) {
            Optional<SessionService.Resolved> session =
                    cookies.readSessionFromHeader(request.getHeaders().getFirst(HttpHeaders.COOKIE))
                            .flatMap(
                                    token ->
                                            sessionService.resolve(
                                                    token,
                                                    request.getHeaders()
                                                            .getFirst(HttpHeaders.USER_AGENT),
                                                    clientAddress(request)));
            if (session.isEmpty()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(PRINCIPAL_ATTRIBUTE, session.get().user().getEmail());
            attributes.put(WebSocketSessionRegistry.SESSION_HASH, session.get().tokenHash());
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request,
                ServerHttpResponse response,
                WebSocketHandler wsHandler,
                Exception exception) {}
    }

    private static String clientAddress(ServerHttpRequest request) {
        return request.getRemoteAddress() == null || request.getRemoteAddress().getAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
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
