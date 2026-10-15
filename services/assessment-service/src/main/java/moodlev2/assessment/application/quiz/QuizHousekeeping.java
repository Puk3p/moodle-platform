package moodlev2.assessment.application.quiz;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps quizzes consistent with users and courses that live in other services. Plain SQL: these are
 * bulk deletes and updates, and the database's own cascades (responses and proctoring events of an
 * attempt) do the rest.
 */
@Service
@RequiredArgsConstructor
public class QuizHousekeeping {

    private final JdbcTemplate jdbc;

    @Transactional
    public void forgetUser(long userId) {
        jdbc.update("DELETE FROM quiz_attempts WHERE user_id = ?", userId);
        jdbc.update("DELETE FROM quiz_extra_attempts WHERE user_id = ?", userId);
    }

    @Transactional
    public void unfileModule(long moduleId) {
        jdbc.update("UPDATE quizzes SET module_id = NULL WHERE module_id = ?", moduleId);
    }
}
