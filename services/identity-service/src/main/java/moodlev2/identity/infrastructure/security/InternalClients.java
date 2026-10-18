package moodlev2.identity.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The services allowed to ask for tokens (client-credentials grant), keyed by client id. A client
 * with a blank secret is disabled, so unset environment variables never open a door.
 *
 * <pre>app.internal.clients.gateway=${INTERNAL_CLIENT_GATEWAY_SECRET:}</pre>
 */
@ConfigurationProperties("app.internal")
public record InternalClients(Map<String, String> clients) {

    public InternalClients {
        clients = clients == null ? Map.of() : Map.copyOf(clients);
    }

    /** Constant-time, so response times do not reveal how much of a secret was right. */
    public boolean authenticates(String clientId, String secret) {
        String expected = clientId == null ? null : clients.get(clientId);
        if (expected == null || expected.isBlank() || secret == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
    }
}
