package moodlev2.gateway.filter;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

/**
 * Where a request comes from. Behind nginx and Docker's port forwarding the gateway's direct peer
 * is always a proxy, so the client is read from {@code X-Forwarded-For}, but only when that peer is
 * a trusted proxy (nginx, the Docker host): anyone else's header is dropped, so it cannot be
 * spoofed.
 */
@Component
public class ClientAddress {

    static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final Pattern trustedProxies;

    public ClientAddress(
            @Value("${spring.cloud.gateway.server.webflux.trusted-proxies}")
                    String trustedProxies) {
        this.trustedProxies = Pattern.compile(trustedProxies);
    }

    /**
     * The addresses the request passed through, the client's first and the peer's own last; empty
     * when the peer is unknown.
     */
    public List<String> chain(ServerHttpRequest request) {
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return List.of();
        }
        String peer = remote.getAddress().getHostAddress();

        List<String> chain = new ArrayList<>();
        if (trusted(peer)) {
            for (String value : request.getHeaders().getOrEmpty(X_FORWARDED_FOR)) {
                for (String part : value.split(",")) {
                    if (!part.isBlank()) {
                        chain.add(part.trim());
                    }
                }
            }
        }
        chain.add(peer);
        return chain;
    }

    /**
     * The client: the right-most address of the chain that is not a trusted proxy, as the services
     * behind the gateway read it. A client can prepend whatever it likes, but the address a proxy
     * appended for it is the one taken. Null when the peer is unknown.
     */
    public String of(ServerHttpRequest request) {
        List<String> chain = chain(request);
        for (int i = chain.size() - 1; i >= 0; i--) {
            if (!trusted(chain.get(i))) {
                return chain.get(i);
            }
        }
        return chain.isEmpty() ? null : chain.get(0);
    }

    private boolean trusted(String address) {
        return trustedProxies.matcher(address).matches();
    }
}
