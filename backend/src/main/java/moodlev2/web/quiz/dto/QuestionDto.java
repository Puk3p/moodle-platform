// moodlev2\web\quiz\dto\QuestionDto.java
package moodlev2.web.quiz.dto;

import java.util.List;

public record QuestionDto(
        String text,
        String type, // SINGLE_CHOICE
        Integer points,
        List<OptionDto> options
) {}