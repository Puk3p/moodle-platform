package moodlev2.assessment.api;

import java.time.Instant;
import java.util.List;

/**
 * Quizzes can block messaging while a student takes them. Messaging asks here which attempts are
 * holding such a lock, and listens for {@link QuizAttemptChangedEvent} to know when to ask again.
 */
public interface ExamLockApi {

    /** The user's in-progress attempts on quizzes that block messaging. */
    List<OpenExam> openMessagingBlockingAttempts(long userId);

    /**
     * @param startedAt when the attempt started; {@code null} for a malformed attempt
     * @param durationMinutes the quiz's time limit, {@code null} or 0 when untimed
     */
    record OpenExam(Instant startedAt, Integer durationMinutes) {}
}
