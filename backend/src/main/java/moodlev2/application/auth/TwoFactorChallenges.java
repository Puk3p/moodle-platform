package moodlev2.application.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.User;
import moodlev2.infrastructure.security.SessionCookies;
import org.springframework.stereotype.Component;

/**
 * The short-lived proof that a user got past their password (or OAuth provider) and still owes a
 * 2FA code. It is signed, scoped to 2FA only (it can never be used as a session), delivered in an
 * HttpOnly cookie, usable once, and dies after {@link #MAX_ATTEMPTS} wrong codes.
 */
@Component
public class TwoFactorChallenges {

    static final String SCOPE = "auth:pre-2fa";
    static final int MAX_ATTEMPTS = 5;

    private static final String EXPIRED = "Your sign-in expired. Please sign in again.";

    private final TokenServicePort tokens;
    private final Map<String, State> states = new ConcurrentHashMap<>();

    public TwoFactorChallenges(TokenServicePort tokens) {
        this.tokens = tokens;
    }

    private static final class State {
        int failures;
        boolean used;
        final Instant expiresAt;

        State(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    public String issue(User user) {
        User subject = new User();
        subject.setId(user.getId());
        subject.setEmail(user.getEmail());
        subject.setRoles(Set.of());
        return tokens.generateToken(subject, SessionCookies.CHALLENGE_LIFETIME, Set.of(SCOPE));
    }

    /** The user a live challenge belongs to; anything else gets one generic error. */
    public Long userIdFor(String token) {
        TokenServicePort.TokenPayload p = parse(token);
        State s = states.computeIfAbsent(p.jti(), k -> new State(p.expiresAt()));
        synchronized (s) {
            if (s.used || s.failures >= MAX_ATTEMPTS) {
                throw new IllegalArgumentException(EXPIRED);
            }
        }
        return p.userId();
    }

    public void recordFailure(String token) {
        State s = states.get(parse(token).jti());
        if (s != null) {
            synchronized (s) {
                s.failures++;
            }
        }
    }

    public void consume(String token) {
        State s = states.get(parse(token).jti());
        if (s != null) {
            synchronized (s) {
                s.used = true;
            }
        }
        cleanup();
    }

    private TokenServicePort.TokenPayload parse(String token) {
        TokenServicePort.TokenPayload p;
        try {
            p = tokens.parse(token);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(EXPIRED);
        }
        if (p.expiresAt().isBefore(Instant.now()) || !p.scopes().contains(SCOPE)) {
            throw new IllegalArgumentException(EXPIRED);
        }
        return p;
    }

    private void cleanup() {
        Instant now = Instant.now();
        states.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
    }
}
