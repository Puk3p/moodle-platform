package moodlev2.web.quiz.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateQuizDto(
        String title,
        String description,
        Long courseId,
        Long moduleId,

        Integer timeLimitMinutes,
        Integer passingScore,
        Integer maxAttempts,
        boolean shuffleOptions,
        String password,
        LocalDateTime availableFrom,
        LocalDateTime availableTo,
        List<Long> assignedClassIds,

        String generationType,

        List<Long> specificQuestionIds,

        List<RandomRuleDto> randomRules
) {
    public record RandomRuleDto(
            Long categoryId,
            String difficulty,
            int count
    ) {}
}