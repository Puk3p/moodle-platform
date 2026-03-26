package moodlev2.infrastructure.mapper;

import java.time.Duration;
import java.time.Instant;
import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import moodlev2.web.course.dto.teacher.TeacherQuizDto;
import org.springframework.stereotype.Component;

@Component
public class QuizMapper {

    public TeacherQuizDto toTeacherDto(QuizEntity entity) {
        String context = (entity.getModule() != null) ? entity.getModule().getTitle() : "";

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
                entity.getCourse().getName(),
                context,
                entity.getStatus(),
                entity.getQuestionsCount(),
                entity.getDurationMinutes(),
                attemptsLabel,
                lastUpdated);
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
