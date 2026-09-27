package moodlev2.application.chat;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;

final class ChatTestData {

    private ChatTestData() {}

    static UserEntity user(long id, String email, Role... roles) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName("First" + id);
        u.setLastName("Last" + id);
        u.setRoles(new HashSet<>(List.of(roles)));
        u.setActive(true);
        return u;
    }

    /** An open attempt on a messaging-blocking quiz, started {@code ago} before now. */
    static QuizAttemptEntity openAttempt(Duration ago, Integer durationMinutes) {
        QuizEntity quiz = new QuizEntity();
        quiz.setDurationMinutes(durationMinutes);
        quiz.setBlockMessaging(true);

        QuizAttemptEntity attempt = new QuizAttemptEntity();
        attempt.setQuiz(quiz);
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(Instant.now().minus(ago));
        return attempt;
    }
}
