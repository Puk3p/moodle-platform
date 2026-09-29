package moodlev2.common.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory "at most N events per key in any rolling window" counter.
 *
 * <p>Deliberately simple: state lives in this JVM only and is lost on restart. That is enough to
 * make brute force and disk-filling loops impractical for a single-node deployment without adding
 * infrastructure. Keys that have gone quiet are swept once the map grows large, so an attacker
 * cycling through keys cannot grow it without bound.
 */
public final class SlidingWindowCounter {

    private static final int SWEEP_THRESHOLD = 50_000;

    private final int max;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    public SlidingWindowCounter(int max, Duration window, Clock clock) {
        if (max < 1) {
            throw new IllegalArgumentException("max must be at least 1");
        }
        this.max = max;
        this.window = window;
        this.clock = clock;
    }

    /** True when the key has already used up its allowance for the current window. */
    public boolean isExhausted(String key) {
        Deque<Instant> times = events.get(key);
        if (times == null) {
            return false;
        }
        synchronized (times) {
            prune(times);
            return times.size() >= max;
        }
    }

    /** Counts one event against the key, whether or not it is already exhausted. */
    public void record(String key) {
        Deque<Instant> times = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (times) {
            prune(times);
            times.addLast(clock.instant());
        }
        sweepIfLarge();
    }

    /**
     * Counts one event if the key still has allowance left. Check and record happen under one lock,
     * so concurrent requests cannot all slip in on the last free slot.
     *
     * @return false (and records nothing) when the allowance is used up
     */
    public boolean tryAcquire(String key) {
        Deque<Instant> times = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        boolean acquired;
        synchronized (times) {
            prune(times);
            acquired = times.size() < max;
            if (acquired) {
                times.addLast(clock.instant());
            }
        }
        sweepIfLarge();
        return acquired;
    }

    /** Forgets everything recorded for the key. */
    public void reset(String key) {
        events.remove(key);
    }

    private void prune(Deque<Instant> times) {
        Instant cutoff = clock.instant().minus(window);
        while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) {
            times.pollFirst();
        }
    }

    private void sweepIfLarge() {
        if (events.size() > SWEEP_THRESHOLD) {
            events.values()
                    .removeIf(
                            times -> {
                                synchronized (times) {
                                    prune(times);
                                    return times.isEmpty();
                                }
                            });
        }
    }
}
