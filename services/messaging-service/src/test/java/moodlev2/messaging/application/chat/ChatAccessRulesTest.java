package moodlev2.messaging.application.chat;

import static moodlev2.messaging.application.chat.ChatTestData.openAttempt;
import static moodlev2.messaging.application.chat.ChatTestData.user;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import moodlev2.messaging.clients.OpenExam;
import moodlev2.messaging.clients.Person;
import org.junit.jupiter.api.Test;

class ChatAccessRulesTest {

    private final Person teacher = user(1, "t@x.com", "TEACHER");
    private final Person otherTeacher = user(2, "t2@x.com", "TEACHER");
    private final Person student = user(3, "s@x.com", "STUDENT");
    private final Person otherStudent = user(4, "s2@x.com", "STUDENT");
    private final Person admin = user(5, "a@x.com", "ADMIN");

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
        Person adminTeacher = user(6, "at@x.com", "ADMIN", "TEACHER");
        assertThat(ChatAccessRules.mayMessage(adminTeacher, student)).isTrue();
    }

    @Test
    void userWithBothRolesCountsAsTeacherSoCannotMessageAnotherTeacher() {
        Person assistant = user(7, "ta@x.com", "STUDENT", "TEACHER");
        assertThat(ChatAccessRules.isTeacher(assistant)).isTrue();
        assertThat(ChatAccessRules.isStudent(assistant)).isFalse();
        assertThat(ChatAccessRules.mayMessage(assistant, student)).isTrue();
        assertThat(ChatAccessRules.mayMessage(assistant, teacher)).isFalse();
    }

    @Test
    void deactivatedAccountsCannotSendOrReceive() {
        Person inactive = ChatTestData.deactivated(user(8, "gone@x.com", "STUDENT"));
        assertThat(ChatAccessRules.mayMessage(teacher, inactive)).isFalse();
        assertThat(ChatAccessRules.mayMessage(inactive, teacher)).isFalse();
    }

    @Test
    void nobodyMessagesThemselves() {
        assertThat(ChatAccessRules.mayMessage(teacher, teacher)).isFalse();
    }

    @Test
    void lockHoldsForTheQuizDurationPlusGrace() {
        OpenExam attempt = openAttempt(Duration.ofMinutes(10), 30);
        Instant expectedEnd =
                attempt.startedAt().plus(Duration.ofMinutes(30)).plus(ChatAccessRules.LOCK_GRACE);

        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now()))
                .contains(expectedEnd);
    }

    @Test
    void lockStillHoldsInsideTheGraceWindow() {
        OpenExam attempt = openAttempt(Duration.ofMinutes(32), 30);
        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now())).isPresent();
    }

    @Test
    void abandonedAttemptStopsLockingOnceItsTimeIsUp() {
        OpenExam attempt = openAttempt(Duration.ofHours(2), 30);
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
        OpenExam attempt = new OpenExam(null, 30);
        assertThat(ChatAccessRules.lockedUntil(List.of(attempt), Instant.now())).isEmpty();
    }

    @Test
    void theLatestEndingAttemptWins() {
        OpenExam shortOne = openAttempt(Duration.ofMinutes(1), 10);
        OpenExam longOne = openAttempt(Duration.ofMinutes(1), 90);

        assertThat(ChatAccessRules.lockedUntil(List.of(shortOne, longOne), Instant.now()))
                .contains(ChatAccessRules.lockEnd(longOne).orElseThrow());
    }

    @Test
    void noOpenAttemptsMeansNoLock() {
        assertThat(ChatAccessRules.lockedUntil(List.of(), Instant.now())).isEmpty();
    }
}
