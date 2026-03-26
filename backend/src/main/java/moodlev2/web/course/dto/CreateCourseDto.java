package moodlev2.web.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCourseDto(
        @NotBlank String code,
        @NotBlank String title,
        String term,
        String description,
        @NotNull Long teacherId) {}
