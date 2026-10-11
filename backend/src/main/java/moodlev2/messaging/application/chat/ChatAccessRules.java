package moodlev2.messaging.application.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import moodlev2.assessment.infrastructure.persistence.entity.QuizAttemptEntity;
import moodlev2.identity.api.UserSummary;

/**
 * Who may message whom, and when messaging is locked. Pure functions over entities so the rules can
 * be tested without a database; {@link ChatService} applies them on every call.
 *
 * <p>Messaging is strictly between one teacher and one student. Student-student, teacher-teacher
 * and anything involving an admin-only account is refused. A user holding both TEACHER and STUDENT
 * (a teaching assistant, say) counts as a teacher.
 */
public final class ChatAccessRules {

    /**
     * Slack after the quiz timer runs out. The client auto-submits at zero, but the request can
     * land a little late; without this, chat would reappear in that gap while the attempt is still
     * open.
     */
    static final Duration LOCK_GRACE = Duration.ofMinutes(5);

    /**
     * Bound for quizzes without a time limit. Attempts can be left IN_PROGRESS forever (a closed
     * window, a crashed browser), and an unbounded lock would then disable a student's chat
     * permanently.
     */
    static final Duration UNTIMED_LOCK_CAP = Duration.ofHours(4);

    private ChatAccessRules() {}

    public static boolean isTeacher(UserSummary user) {
        return user.isTeacher();
    }

    public static boolean isStudent(UserSummary user) {
        return user.isStudent();
    }

    /** Whether this account takes part in messaging at all. */
    public static boolean canUseChat(UserSummary user) {
        return user.active() && (isTeacher(user) || isStudent(user));
    }

    /** Exactly one side is a teacher; the other is then necessarily a student. */
    public static boolean mayMessage(UserSummary from, UserSummary to) {
        if (Objects.equals(from.id(), to.id())) {
            return false;
        }
        return canUseChat(from) && canUseChat(to) && isTeacher(from) != isTeacher(to);
    }

    /**
     * When the messaging lock from the given open, messaging-blocking attempts ends, or empty if
     * none of them still holds it. Only meaningful for students; callers skip teachers, who may
     * start attempts to preview their own quizzes.
     */
    public static Optional<Instant> lockedUntil(
            Collection<QuizAttemptEntity> openBlockingAttempts, Instant now) {
        return openBlockingAttempts.stream()
                .map(ChatAccessRules::lockEnd)
                .flatMap(Optional::stream)
                .filter(end -> end.isAfter(now))
                .max(Instant::compareTo);
    }

    /** The time budget of an attempt, plus grace; attempts without a start time never lock. */
    static Optional<Instant> lockEnd(QuizAttemptEntity attempt) {
        Instant started = attempt.getStartedAt();
        if (started == null) {
            return Optional.empty();
        }
        Integer minutes = attempt.getQuiz().getDurationMinutes();
        if (minutes == null || minutes <= 0) {
            return Optional.of(started.plus(UNTIMED_LOCK_CAP));
        }
        return Optional.of(started.plus(Duration.ofMinutes(minutes)).plus(LOCK_GRACE));
    }
}
