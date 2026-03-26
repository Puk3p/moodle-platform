package moodlev2.web.questionbank.dto;

import java.util.List;

public record CreateQuestionRequest(
        String text,
        String type,
        String difficulty,
        Long categoryId,
        List<String> tags,
        List<OptionDto> options) {
    public record OptionDto(String text, boolean isCorrect) {}
}
