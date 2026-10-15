package moodlev2.gateway.filter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import moodlev2.gateway.session.ResolvedSession;
import moodlev2.gateway.session.SessionResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * {@code SessionToken}: authenticates a request to a token-only service by its session cookie and
 * forwards it with the user token identity-service issued instead. The service never sees the
 * cookie, and a client cannot smuggle its own {@code Authorization} header through: it is always
 * replaced.
 *
 * <p>No or unaccepted session: 401, the same answer identity-service gives its own routes.
 */
@Component
public class SessionTokenGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    private final SessionResolver sessions;
    private final String sessionCookie;

    public SessionTokenGatewayFilterFactory(
            SessionResolver sessions,
            @Value("${app.session.cookie-name:__Host-mdl_session}") String sessionCookie) {
        super(Object.class);
        this.sessions = sessions;
        this.sessionCookie = sessionCookie;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            HttpCookie cookie = request.getCookies().getFirst(sessionCookie);
            if (cookie == null || cookie.getValue().isBlank()) {
                return unauthorized(exchange);
            }
            return sessions.resolve(
                            cookie.getValue(),
                            request.getHeaders().getFirst(HttpHeaders.USER_AGENT),
                            clientAddress(request))
                    .map(Optional::of)
                    .defaultIfEmpty(Optional.empty())
                    .flatMap(
                            resolved ->
                                    resolved.isPresent()
                                            ? chain.filter(withToken(exchange, resolved.get()))
                                            : unauthorized(exchange));
        };
    }

    private static ServerWebExchange withToken(
            ServerWebExchange exchange, ResolvedSession session) {
        ServerHttpRequest request =
                exchange.getRequest()
                        .mutate()
                        .headers(
                                h -> {
                                    h.remove(HttpHeaders.COOKIE);
                                    h.setBearerAuth(session.accessToken());
                                })
                        .build();
        return exchange.mutate().request(request).build();
    }

    private static String clientAddress(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null || remote.getAddress() == null
                ? null
                : remote.getAddress().getHostAddress();
    }

    static Mono<Void> unauthorized(ServerWebExchange exchange) {
        return reject(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    static Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String error) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + error + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
