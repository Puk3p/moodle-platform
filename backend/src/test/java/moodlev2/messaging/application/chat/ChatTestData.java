package moodlev2.messaging.application.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import moodlev2.assessment.api.ExamLockApi.OpenExam;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserSummary;

final class ChatTestData {

    private ChatTestData() {}

    static UserSummary user(long id, String email, Role... roles) {
        return new UserSummary(
                id, email, "First" + id, "Last" + id, Set.of(roles), null, null, true);
    }

    static UserSummary deactivated(UserSummary u) {
        return new UserSummary(
                u.id(),
                u.email(),
                u.firstName(),
                u.lastName(),
                u.roles(),
                u.classId(),
                u.className(),
                false);
    }

    /** An open attempt on a messaging-blocking quiz, started {@code ago} before now. */
    static OpenExam openAttempt(Duration ago, Integer durationMinutes) {
        return new OpenExam(Instant.now().minus(ago), durationMinutes);
    }
}
