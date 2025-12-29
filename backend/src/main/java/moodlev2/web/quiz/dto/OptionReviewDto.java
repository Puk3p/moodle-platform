package moodlev2.web.quiz.dto;

public record OptionReviewDto(
        Long id,
        String text,
        boolean isSelected,
        boolean isCorrectAnswer
) {}