package moodlev2.application.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Per-account throttling, on top of the per-IP limit in {@code RateLimitFilter}: an attacker
 * rotating IP addresses still gets only {@link #MAX_FAILURES} guesses per account per window.
 *
 * <p>Keys are tracked whether or not the account exists, and the lock message is the same for both,
 * so the lock itself does not reveal which email addresses are registered.
 */
@Component
public class LoginAttemptGuard {

    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public LoginAttemptGuard() {
        this(Clock.systemUTC());
    }

    LoginAttemptGuard(Clock clock) {
        this.clock = clock;
    }

    public void checkAllowed(String kind, String account) {
        Deque<Instant> times = failures.get(key(kind, account));
        if (times == null) {
            return;
        }
        synchronized (times) {
            prune(times);
            if (times.size() >= MAX_FAILURES) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many attempts. Try again in 15 minutes.");
            }
        }
    }

    public void recordFailure(String kind, String account) {
        Deque<Instant> times =
                failures.computeIfAbsent(key(kind, account), k -> new ArrayDeque<>());
        synchronized (times) {
            prune(times);
            times.addLast(clock.instant());
        }
        if (failures.size() > 50_000) {
            failures.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }

    public void recordSuccess(String kind, String account) {
        failures.remove(key(kind, account));
    }

    private void prune(Deque<Instant> times) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.pollFirst();
        }
    }

    private static String key(String kind, String account) {
        return kind + ":" + (account == null ? "" : account.trim().toLowerCase(Locale.ROOT));
    }
}
