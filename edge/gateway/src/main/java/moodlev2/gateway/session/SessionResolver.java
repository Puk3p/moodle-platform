package moodlev2.gateway.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Exchanges a browser's session cookie for a user token, by asking identity-service (which applies
 * its usual session checks: expiry, idle time, same browser, active account).
 *
 * <p>Answers are cached briefly to spare identity-service a round trip per request. The cache key
 * includes the User-Agent, so a cookie replayed from another browser is never served a cached
 * token: it goes to identity-service, which refuses it (and revokes the session). A revoked session
 * or a changed account is evicted at once from identity-service's events ({@link
 * SessionEventsListener}); the TTL only bounds staleness of what has no event (a role change).
 */
@Component
public class SessionResolver {

    private final WebClient identity;
    private final Cache<CacheKey, ResolvedSession> cache;

    public SessionResolver(
            WebClient identityWebClient, @Value("${app.session.cache-ttl:20s}") Duration ttl) {
        this.identity = identityWebClient;
        this.cache = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(100_000).build();
    }

    /** Empty when identity-service does not accept the session. */
    public Mono<ResolvedSession> resolve(String token, String userAgent, String remoteAddress) {
        CacheKey key = new CacheKey(sha256(token), sha256(userAgent == null ? "" : userAgent));
        ResolvedSession cached = cache.getIfPresent(key);
        if (cached != null) {
            return Mono.just(cached);
        }
        return identity.post()
                .uri("/internal/sessions/resolve")
                .bodyValue(
                        Map.of(
                                "token", token,
                                "userAgent", userAgent == null ? "" : userAgent,
                                "remoteAddress", remoteAddress == null ? "" : remoteAddress))
                .retrieve()
                .bodyToMono(ResolvedSession.class)
                .doOnNext(resolved -> cache.put(key, resolved))
                .onErrorResume(
                        WebClientResponseException.class,
                        e ->
                                e.getStatusCode() == HttpStatus.UNAUTHORIZED
                                        ? Mono.empty()
                                        : Mono.error(e));
    }

    /** Sessions identity-service revoked (logout, password change, theft detection...). */
    public void evictSessions(Collection<String> tokenHashes) {
        Set<String> doomed = Set.copyOf(tokenHashes);
        cache.asMap().keySet().removeIf(k -> doomed.contains(k.tokenHash()));
    }

    /** Every cached session of an account that changed (email, deletion). */
    public void evictUser(long userId) {
        cache.asMap().values().removeIf(s -> s.userId() == userId);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private record CacheKey(String tokenHash, String userAgentHash) {}
}
