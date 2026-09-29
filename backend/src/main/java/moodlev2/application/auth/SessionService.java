package moodlev2.application.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import moodlev2.common.util.TokenHashUtil;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-side sessions behind the HttpOnly session cookie.
 *
 * <p>A session is a 256-bit random token held only by the browser (JavaScript cannot read the
 * cookie); the database keeps its SHA-256. On every request the session is checked for:
 *
 * <ul>
 *   <li>absolute expiry ({@link #ABSOLUTE_LIFETIME}) and idle expiry ({@link #IDLE_TIMEOUT});
 *   <li>the browser: the User-Agent must match the one that signed in, so a cookie copied to
 *       another browser (a "cookie grabber") is rejected and the session is revoked outright;
 *   <li>the account: a deactivated user loses every session immediately, and roles are read fresh
 *       from the database, so a demotion takes effect on the next request.
 * </ul>
 *
 * <p>Revocations publish {@link SessionsRevokedEvent} so open WebSocket connections using those
 * sessions are closed too.
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    public static final Duration ABSOLUTE_LIFETIME = Duration.ofHours(12);

    /** Long enough for a timed quiz with no other traffic; the quiz page also reports activity. */
    public static final Duration IDLE_TIMEOUT = Duration.ofHours(2);

    /** last_active is written at most this often, not on every request. */
    static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);

    /** Oldest sessions are evicted beyond this, so a leaked password cannot pile up sessions. */
    static final int MAX_SESSIONS_PER_USER = 10;

    private static final int TOKEN_BYTES = 32;

    private final UserSessionRepository sessions;
    private final SpringDataUserRepository users;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SessionService(
            UserSessionRepository sessions,
            SpringDataUserRepository users,
            ApplicationEventPublisher events) {
        this(sessions, users, events, Clock.systemUTC());
    }

    SessionService(
            UserSessionRepository sessions,
            SpringDataUserRepository users,
            ApplicationEventPublisher events,
            Clock clock) {
        this.sessions = sessions;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    /**
     * @param token the raw value for the cookie; never stored or logged
     */
    public record Issued(String token, Duration maxAge) {}

    /** A valid, current session and the account it belongs to. */
    public record Resolved(Long sessionId, String tokenHash, UserEntity user) {}

    @Transactional
    public Issued create(Long userId, String ipAddress, String userAgent) {
        UserEntity user =
                users.findById(userId)
                        .orElseThrow(() -> new IllegalStateException("Unknown user " + userId));
        Instant now = clock.instant();

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        UserSessionEntity session = new UserSessionEntity();
        session.setUser(user);
        session.setTokenSignature(TokenHashUtil.sha256(token));
        session.setUserAgentHash(hashUserAgent(userAgent));
        session.setDeviceName(deviceName(userAgent));
        session.setIpAddress(ipAddress);
        session.setCreatedAt(now);
        session.setLastActive(now);
        session.setExpiresAt(now.plus(ABSOLUTE_LIFETIME));
        sessions.save(session);

        evictBeyondLimit(userId);
        return new Issued(token, ABSOLUTE_LIFETIME);
    }

    /**
     * Validates a cookie token for this request. Any failed check deletes the session, so a stolen
     * or stale cookie stops working everywhere, not just for this request.
     */
    @Transactional
    public Optional<Resolved> resolve(String token, String userAgent, String ipAddress) {
        if (token == null || token.isBlank() || token.length() > 128) {
            return Optional.empty();
        }
        String hash = TokenHashUtil.sha256(token);
        Optional<UserSessionEntity> found = sessions.findWithUserByTokenSignature(hash);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UserSessionEntity s = found.get();
        Instant now = clock.instant();

        String reason = null;
        if (s.getExpiresAt() == null || !now.isBefore(s.getExpiresAt())) {
            reason = "expired";
        } else if (s.getLastActive() == null
                || !now.isBefore(s.getLastActive().plus(IDLE_TIMEOUT))) {
            reason = "idle";
        } else if (!Objects.equals(s.getUserAgentHash(), hashUserAgent(userAgent))) {
            // The strongest signal we have that the cookie left the browser it was issued to.
            log.warn(
                    "Session {} of user {} presented by a different browser from {}; revoked",
                    s.getId(),
                    s.getUser().getId(),
                    ipAddress);
            reason = "browser mismatch";
        } else if (!s.getUser().isActive()) {
            reason = "account disabled";
        }

        if (reason != null) {
            delete(List.of(s));
            return Optional.empty();
        }

        if (s.getLastActive().plus(TOUCH_INTERVAL).isBefore(now)
                || !Objects.equals(s.getIpAddress(), ipAddress)) {
            s.setLastActive(now);
            s.setIpAddress(ipAddress);
            sessions.save(s);
        }
        return Optional.of(new Resolved(s.getId(), hash, s.getUser()));
    }

    @Transactional
    public void revokeByHash(String tokenHash) {
        sessions.findWithUserByTokenSignature(tokenHash).ifPresent(s -> delete(List.of(s)));
    }

    @Transactional
    public void revokeById(Long sessionId, Long userId) {
        sessions.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .ifPresent(s -> delete(List.of(s)));
    }

    /** Signs a user out everywhere (password reset, account takeover response). */
    @Transactional
    public void revokeAll(Long userId) {
        delete(sessions.findAllByUserIdOrderByCreatedAtAsc(userId));
    }

    /** Signs a user out everywhere except the session making the request. */
    @Transactional
    public void revokeOthers(Long userId, String keepTokenHash) {
        delete(
                sessions.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                        .filter(s -> !s.getTokenSignature().equals(keepTokenHash))
                        .toList());
    }

    private void evictBeyondLimit(Long userId) {
        List<UserSessionEntity> all = sessions.findAllByUserIdOrderByCreatedAtAsc(userId);
        int excess = all.size() - MAX_SESSIONS_PER_USER;
        if (excess > 0) {
            delete(all.subList(0, excess));
        }
    }

    private void delete(List<UserSessionEntity> doomed) {
        if (doomed.isEmpty()) {
            return;
        }
        sessions.deleteAll(doomed);
        events.publishEvent(
                new SessionsRevokedEvent(
                        doomed.stream().map(UserSessionEntity::getTokenSignature).toList()));
    }

    static String hashUserAgent(String userAgent) {
        return TokenHashUtil.sha256(userAgent == null ? "" : userAgent);
    }

    private static String deviceName(String ua) {
        if (ua == null) {
            return "Unknown device";
        }
        if (ua.contains("iPhone") || ua.contains("iPad")) {
            return "iOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Windows")) {
            return "Windows";
        }
        if (ua.contains("Mac")) {
            return "macOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "Unknown device";
    }

    /** Hashes of sessions that no longer exist; listeners close anything still using them. */
    public record SessionsRevokedEvent(List<String> tokenHashes) {}
}
