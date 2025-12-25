// moodlev2\web\quiz\dto\OptionDto.java
package moodlev2.web.quiz.dto;

public record OptionDto(
        String text,
        boolean isCorrect
) {}