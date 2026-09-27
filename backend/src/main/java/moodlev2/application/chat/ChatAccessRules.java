package moodlev2.application.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;

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

    public static boolean isTeacher(UserEntity user) {
        return user.getRoles().contains(Role.TEACHER);
    }

    public static boolean isStudent(UserEntity user) {
        return user.getRoles().contains(Role.STUDENT) && !isTeacher(user);
    }

    /** Whether this account takes part in messaging at all. */
    public static boolean canUseChat(UserEntity user) {
        return user.isActive() && (isTeacher(user) || isStudent(user));
    }

    /** Exactly one side is a teacher; the other is then necessarily a student. */
    public static boolean mayMessage(UserEntity from, UserEntity to) {
        if (Objects.equals(from.getId(), to.getId())) {
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
