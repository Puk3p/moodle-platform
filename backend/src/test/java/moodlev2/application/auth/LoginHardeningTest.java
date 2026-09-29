package moodlev2.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.infrastructure.security.JwtServiceAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LoginHardeningTest {

    private final TokenServicePort tokens =
            new JwtServiceAdapter("test-secret-that-is-long-enough-for-hs256!!", "moodlev2");

    private static User alex() {
        User u = new User();
        u.setId(1L);
        u.setEmail("student@test.com");
        u.setRoles(Set.of(Role.STUDENT));
        return u;
    }

    // ── Per-account lockout ──────────────────────────────────────────────────

    @Test
    void anAccountLocksAfterFiveFailuresRegardlessOfWhichIpTheyCameFrom() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        for (int i = 0; i < LoginAttemptGuard.MAX_FAILURES; i++) {
            guard.checkAllowed("password", "student@test.com");
            guard.recordFailure("password", "Student@Test.com "); // normalised
        }

        assertThatThrownBy(() -> guard.checkAllowed("password", "student@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Too many attempts");
        // Another account is unaffected.
        assertThatCode(() -> guard.checkAllowed("password", "teacher@test.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void theLockLiftsAfterTheWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-29T10:00:00Z"));
        LoginAttemptGuard guard = new LoginAttemptGuard(clock);
        for (int i = 0; i < LoginAttemptGuard.MAX_FAILURES; i++) {
            guard.recordFailure("password", "a@b.c");
        }

        clock.advance(LoginAttemptGuard.WINDOW.plusSeconds(1));

        assertThatCode(() -> guard.checkAllowed("password", "a@b.c")).doesNotThrowAnyException();
    }

    @Test
    void aSuccessfulSignInClearsTheCounter() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        for (int i = 0; i < LoginAttemptGuard.MAX_FAILURES - 1; i++) {
            guard.recordFailure("password", "a@b.c");
        }
        guard.recordSuccess("password", "a@b.c");
        guard.recordFailure("password", "a@b.c");

        assertThatCode(() -> guard.checkAllowed("password", "a@b.c")).doesNotThrowAnyException();
    }

    // ── Pre-2FA challenges ───────────────────────────────────────────────────

    @Test
    void aChallengeIdentifiesItsUser() {
        TwoFactorChallenges challenges = new TwoFactorChallenges(tokens);
        String challenge = challenges.issue(alex());

        assertThat(challenges.userIdFor(challenge)).isEqualTo(1L);
    }

    @Test
    void aTokenWithoutThe2faScopeIsNotAChallenge() {
        TwoFactorChallenges challenges = new TwoFactorChallenges(tokens);
        String accessLike =
                tokens.generateToken(alex(), Duration.ofMinutes(5), Set.of("access:api"));

        assertThatThrownBy(() -> challenges.userIdFor(accessLike))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aChallengeDiesAfterFiveWrongCodes() {
        TwoFactorChallenges challenges = new TwoFactorChallenges(tokens);
        String challenge = challenges.issue(alex());
        for (int i = 0; i < TwoFactorChallenges.MAX_ATTEMPTS; i++) {
            challenges.userIdFor(challenge);
            challenges.recordFailure(challenge);
        }

        assertThatThrownBy(() -> challenges.userIdFor(challenge))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aChallengeCanOnlyBeUsedOnce() {
        TwoFactorChallenges challenges = new TwoFactorChallenges(tokens);
        String challenge = challenges.issue(alex());
        challenges.userIdFor(challenge);
        challenges.consume(challenge);

        assertThatThrownBy(() -> challenges.userIdFor(challenge))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aForgedOrTamperedChallengeIsRejected() {
        TwoFactorChallenges challenges = new TwoFactorChallenges(tokens);
        String challenge = challenges.issue(alex());
        String tampered = challenge.substring(0, challenge.length() - 3) + "abc";
        TokenServicePort otherKey =
                new JwtServiceAdapter("another-secret-that-is-long-enough-for-it", "moodlev2");
        String forged =
                otherKey.generateToken(
                        alex(), Duration.ofMinutes(5), Set.of(TwoFactorChallenges.SCOPE));

        assertThatThrownBy(() -> challenges.userIdFor(tampered))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> challenges.userIdFor(forged))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
