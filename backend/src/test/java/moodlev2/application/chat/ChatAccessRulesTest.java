package moodlev2.application.chat;

import static moodlev2.application.chat.ChatTestData.openAttempt;
import static moodlev2.application.chat.ChatTestData.user;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.junit.jupiter.api.Test;

class ChatAccessRulesTest {

    private final UserEntity teacher = user(1, "t@x.com", Role.TEACHER);
    private final UserEntity otherTeacher = user(2, "t2@x.com", Role.TEACHER);
    private final UserEntity student = user(3, "s@x.com", Role.STUDENT);
    private final UserEntity otherStudent = user(4, "s2@x.com", Role.STUDENT);
    private final UserEntity admin = user(5, "a@x.com", Role.ADMIN);

    @Test
    void teacherAndStudentMayMessageEachOtherInBothDirections() {
        assertThat(ChatAccessRules.mayMessage(teacher, student)).isTrue();
        assertThat(ChatAccessRules.mayMessage(student, teacher)).isTrue();
    }

    @Test
    void studentToStudentIsRefused() {
        assertThat(ChatAccessRules.mayMessage(student, otherStudent)).isFalse();
    }

    @Test
    void teacherToTeacherIsRefused() {
        assertThat(ChatAccessRules.mayMessage(teacher, otherTeacher)).isFalse();
    }

    @Test
    void adminOnlyAccountsAreOutsideMessaging() {
        assertThat(ChatAccessRules.canUseChat(admin)).isFalse();
        assertThat(ChatAccessRules.mayMessage(admin, student)).isFalse();
        assertThat(ChatAccessRules.mayMessage(student, admin)).isFalse();
    }

    @Test
    void adminWhoAlsoTeachesCountsAsTeacher() {
        UserEntity adminTeacher = user(6, "at@x.com", Role.ADMIN, Role.TEACHER);
        assertThat(ChatAccessRules.mayMessage(adminTeacher, student)).isTrue();
    }

    @Test
    void userWithBothRolesCountsAsTeacherSoCannotMessageAnotherTeacher() {
        UserEntity assistant = user(7, "ta@x.com", Role.STUDENT, Role.TEACHER);
        assertThat(ChatAccessRules.isTeacher(assistant)).isTrue();
        assertThat(ChatAccessRules.isStudent(assistant)).isFalse();
        assertThat(ChatAccessRules.mayMessage(assistant, student)).isTrue();
        assertThat(ChatAccessRules.mayMessage(assistant, teacher)).isFalse();
    }

    @Test
    void deactivatedAccountsCannotSendOrReceive() {
        UserEntity inactive = user(8, "gone@x.com", Role.STUDENT);
        inactive.setActive(false);
        assertThat(ChatAccessRules.mayMessage(teacher, inactive)).isFalse();
        assertThat(ChatAccessRules.mayMessage(inactive, teacher)).isFalse();
    }

    @Test
    void nobodyMessagesThemselves() {
        assertThat(ChatAccessRules.mayMessage(teacher, teacher)).isFalse();
    }

    @Test
    void lockHoldsForTheQuizDurationPlusGrace() {
        QuizAttemptEntity attempt = openAttempt(Duration.ofMinutes(10), 30);
        Instant expectedEnd =
                attempt.getStartedAt()
                        .plus(Duration.ofMinutes(30))
                        .plus(ChatAccessRules.LOCK_GRACE);

        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now()))
                .contains(expectedEnd);
    }

    @Test
    void lockStillHoldsInsideTheGraceWindow() {
        QuizAttemptEntity attempt = openAttempt(Duration.ofMinutes(32), 30);
        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now())).isPresent();
    }

    @Test
    void abandonedAttemptStopsLockingOnceItsTimeIsUp() {
        QuizAttemptEntity attempt = openAttempt(Duration.ofHours(2), 30);
        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now())).isEmpty();
    }

    @Test
    void untimedQuizLocksUpToTheCapThenReleases() {
        assertThat(
                        ChatAccessRules.lockedUntil(
                                List.of(openAttempt(Duration.ofHours(1), null)), Instant.now()))
                .isPresent();
        assertThat(
                        ChatAccessRules.lockedUntil(
                                List.of(openAttempt(Duration.ofHours(5), 0)), Instant.now()))
                .isEmpty();
    }

    @Test
    void attemptWithoutStartTimeNeverLocks() {
        QuizAttemptEntity attempt = openAttempt(Duration.ZERO, 30);
        attempt.setStartedAt(null);
        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now())).isEmpty();
    }

    @Test
    void theLatestEndingAttemptWins() {
        QuizAttemptEntity shortOne = openAttempt(Duration.ofMinutes(1), 10);
        QuizAttemptEntity longOne = openAttempt(Duration.ofMinutes(1), 90);

        assertThat(ChatAccessRules.lockedUntil(List.of(shortOne, longOne), Instant.now()))
                .contains(ChatAccessRules.lockEnd(longOne).orElseThrow());
    }

    @Test
    void noOpenAttemptsMeansNoLock() {
        assertThat(ChatAccessRules.lockedUntil(List.of(), Instant.now())).isEmpty();
    }
}
