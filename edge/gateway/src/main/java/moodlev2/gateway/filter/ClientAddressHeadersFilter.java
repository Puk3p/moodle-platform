package moodlev2.gateway.filter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Writes {@code X-Forwarded-For} on proxied requests so services see the real client address (core
 * rate-limits sign-in per address and records it per session).
 *
 * <p>The chain a client sends is kept only when the gateway's direct peer is a trusted proxy
 * (nginx, the Docker host); anyone else's header is dropped, so it cannot be spoofed. The peer's
 * own address is always appended. A service behind the gateway then takes the right-most address
 * that is not a proxy, which is the client.
 *
 * <p>The gateway's built-in handling is switched off for this header: since 4.3 it also drops every
 * entry that is not itself a trusted proxy, which removes the client address it exists to carry.
 */
@Component
public class ClientAddressHeadersFilter implements HttpHeadersFilter, Ordered {

    static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final Pattern trustedProxies;

    public ClientAddressHeadersFilter(
            @Value("${spring.cloud.gateway.server.webflux.trusted-proxies}")
                    String trustedProxies) {
        this.trustedProxies = Pattern.compile(trustedProxies);
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return input;
        }
        String peer = remote.getAddress().getHostAddress();

        List<String> chain = new ArrayList<>();
        if (trustedProxies.matcher(peer).matches()) {
            for (String value : exchange.getRequest().getHeaders().getOrEmpty(X_FORWARDED_FOR)) {
                for (String part : value.split(",")) {
                    if (!part.isBlank()) {
                        chain.add(part.trim());
                    }
                }
            }
        }
        chain.add(peer);

        HttpHeaders headers = new HttpHeaders();
        headers.putAll(input);
        headers.set(X_FORWARDED_FOR, String.join(", ", chain));
        return headers;
    }

    /** After the built-in X-Forwarded-* filter, which still writes Proto, Host and Port. */
    @Override
    public int getOrder() {
        return 1;
    }
}
