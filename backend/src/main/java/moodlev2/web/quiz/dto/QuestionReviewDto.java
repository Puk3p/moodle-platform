package moodlev2.web.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuestionReviewDto(
        Long questionId,
        String text,
        String type,
        BigDecimal pointsAwarded,
        BigDecimal maxPoints,
        boolean isCorrect,
        List<OptionReviewDto> options,
        String feedback // Optional
) {}