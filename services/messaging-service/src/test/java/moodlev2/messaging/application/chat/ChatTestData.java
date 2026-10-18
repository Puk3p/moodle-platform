package moodlev2.messaging.application.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import moodlev2.messaging.clients.OpenExam;
import moodlev2.messaging.clients.Person;

final class ChatTestData {

    private ChatTestData() {}

    static Person user(long id, String email, String... roles) {
        return new Person(id, email, "First" + id, "Last" + id, Set.of(roles), null, null, true);
    }

    static Person deactivated(Person u) {
        return new Person(
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
