package moodlev2.messaging.core;

import java.time.Instant;

/**
 * A student's in-progress attempt on a quiz that blocks messaging.
 *
 * @param startedAt {@code null} for a malformed attempt (it never locks)
 * @param durationMinutes the time limit, {@code null} or 0 when untimed
 */
public record OpenExam(Instant startedAt, Integer durationMinutes) {}
