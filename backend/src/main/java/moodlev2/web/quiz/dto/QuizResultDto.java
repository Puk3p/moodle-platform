package moodlev2.web.quiz.dto;

import java.math.BigDecimal;

public record QuizResultDto(
        Long attemptId,
        String quizTitle,
        BigDecimal score,
        BigDecimal maxScore,
        boolean passed,
        String completedAt) {}
