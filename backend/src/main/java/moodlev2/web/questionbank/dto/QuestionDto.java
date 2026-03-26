package moodlev2.web.questionbank.dto;

import java.util.List;

public record QuestionDto(
        String id,
        String text,
        List<String> tags,
        String type,
        String difficulty,
        int usageCount,
        String categoryId,
        List<OptionDto> options) {
    public record OptionDto(String text, boolean isCorrect) {}
}
