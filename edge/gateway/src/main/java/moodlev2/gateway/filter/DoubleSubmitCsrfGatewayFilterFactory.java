package moodlev2.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * {@code DoubleSubmitCsrf}: CSRF protection for split-out services, matching what core does on its
 * own routes. The SPA reads the XSRF cookie core sets and echoes it in {@code X-XSRF-TOKEN}; a
 * cross-site page can do neither, so a state-changing request without a matching header is refused
 * with 403. Safe methods pass.
 */
@Component
public class DoubleSubmitCsrfGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    static final String HEADER = "X-XSRF-TOKEN";
    private static final Set<String> COOKIES = Set.of("__Host-XSRF-TOKEN", "XSRF-TOKEN");
    private static final Set<HttpMethod> SAFE =
            Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);

    public DoubleSubmitCsrfGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            if (SAFE.contains(request.getMethod())) {
                return chain.filter(exchange);
            }
            String header = request.getHeaders().getFirst(HEADER);
            String cookie =
                    COOKIES.stream()
                            .map(name -> request.getCookies().getFirst(name))
                            .filter(c -> c != null && !c.getValue().isBlank())
                            .map(HttpCookie::getValue)
                            .findFirst()
                            .orElse(null);
            if (header == null || cookie == null || !constantTimeEquals(header, cookie)) {
                return SessionTokenGatewayFilterFactory.reject(
                        exchange, HttpStatus.FORBIDDEN, "Forbidden");
            }
            return chain.filter(exchange);
        };
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
