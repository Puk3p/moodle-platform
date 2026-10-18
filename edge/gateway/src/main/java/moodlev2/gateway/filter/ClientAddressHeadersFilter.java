package moodlev2.gateway.filter;

import static moodlev2.gateway.filter.ClientAddress.X_FORWARDED_FOR;

import java.util.List;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * Writes {@code X-Forwarded-For} on proxied requests so services see the real client address
 * (identity-service rate-limits sign-in per address and records it per session).
 *
 * <p>The chain is the one {@link ClientAddress} believes: what a trusted proxy reported, then the
 * peer's own address. A service behind the gateway then takes the right-most address that is not a
 * proxy, which is the client.
 *
 * <p>The gateway's built-in handling is switched off for this header: since 4.3 it also drops every
 * entry that is not itself a trusted proxy, which removes the client address it exists to carry.
 */
@Component
public class ClientAddressHeadersFilter implements HttpHeadersFilter, Ordered {

    private final ClientAddress clientAddress;

    public ClientAddressHeadersFilter(ClientAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    @Override
    public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
        List<String> chain = clientAddress.chain(exchange.getRequest());
        if (chain.isEmpty()) {
            return input;
        }
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
