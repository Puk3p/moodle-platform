package moodlev2.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import moodlev2.support.Fixtures.MutableClock;
import org.junit.jupiter.api.Test;

class SlidingWindowCounterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-29T10:00:00Z"));
    private final SlidingWindowCounter counter =
            new SlidingWindowCounter(3, Duration.ofMinutes(15), clock);

    @Test
    void exhaustedOnceTheAllowanceIsRecorded() {
        counter.record("k");
        counter.record("k");
        assertThat(counter.isExhausted("k")).isFalse();

        counter.record("k");
        assertThat(counter.isExhausted("k")).isTrue();
    }

    @Test
    void eventsOlderThanTheWindowStopCounting() {
        counter.record("k");
        counter.record("k");
        counter.record("k");

        clock.advance(Duration.ofMinutes(15));

        assertThat(counter.isExhausted("k")).isFalse();
    }

    @Test
    void theWindowRollsRatherThanResettingAllAtOnce() {
        counter.record("k");
        clock.advance(Duration.ofMinutes(10));
        counter.record("k");
        counter.record("k");

        clock.advance(Duration.ofMinutes(6));

        // The first event has aged out, the two recent ones still count.
        assertThat(counter.tryAcquire("k")).isTrue();
        assertThat(counter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquireRecordsNothingOnceExhausted() {
        assertThat(counter.tryAcquire("k")).isTrue();
        assertThat(counter.tryAcquire("k")).isTrue();
        assertThat(counter.tryAcquire("k")).isTrue();
        assertThat(counter.tryAcquire("k")).isFalse();

        // Refused calls did not extend the lock-out.
        clock.advance(Duration.ofMinutes(15));
        assertThat(counter.tryAcquire("k")).isTrue();
    }

    @Test
    void keysAreIndependentAndResetClearsOne() {
        counter.record("a");
        counter.record("a");
        counter.record("a");

        assertThat(counter.isExhausted("b")).isFalse();

        counter.reset("a");
        assertThat(counter.isExhausted("a")).isFalse();
    }
}
