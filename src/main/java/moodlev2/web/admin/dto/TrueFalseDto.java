package moodlev2.web.admin.dto;

import jakarta.validation.constraints.NotNull;

public class TrueFalseDto extends QuestionDto {
    @NotNull public Boolean answer;
}
