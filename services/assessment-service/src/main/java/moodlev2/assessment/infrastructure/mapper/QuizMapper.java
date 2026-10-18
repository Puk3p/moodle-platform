package moodlev2.assessment.infrastructure.mapper;

import java.time.Duration;
import java.time.Instant;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import moodlev2.assessment.web.quiz.dto.TeacherQuizDto;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

    /**
     * @param courseName the quiz's course
     * @param moduleTitle the module it is filed under, {@code ""} for none
     */
    public TeacherQuizDto toTeacherDto(QuizEntity entity, String courseName, String moduleTitle) {
        String context = moduleTitle;

        String attemptsLabel;
        if (entity.getMaxAttempts() == null || entity.getMaxAttempts() == 0) {
            attemptsLabel = "Unlimited";
        } else if (entity.getMaxAttempts() == 1) {
            attemptsLabel = "1 Attempt";
        } else {
            attemptsLabel = entity.getMaxAttempts() + " Attempts";
        }

        // Time Ago
        String lastUpdated = "Updated " + calculateTimeAgo(entity.getUpdatedAt());

        return new TeacherQuizDto(
                String.valueOf(entity.getId()),
                entity.getTitle(),
                courseName,
                context,
                entity.getStatus(),
                entity.getQuestionsCount() != null ? entity.getQuestionsCount() : 0,
                // Untimed quizzes have no duration; 0 rather than a null unboxing failure.
                entity.getDurationMinutes() != null ? entity.getDurationMinutes() : 0,
                attemptsLabel,
                lastUpdated,
                entity.isBlockMessaging());
    }

    private String calculateTimeAgo(Instant time) {
        if (time == null) return "recently";
        long diffSeconds = Duration.between(time, Instant.now()).getSeconds();

        if (diffSeconds < 60) return "just now";
        long diffMinutes = diffSeconds / 60;
        if (diffMinutes < 60) return diffMinutes + " mins ago";
        long diffHours = diffMinutes / 60;
        if (diffHours < 24) return diffHours + " hours ago";
        long diffDays = diffHours / 24;
        return diffDays + " days ago";
    }
}
