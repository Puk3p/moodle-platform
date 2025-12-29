package moodlev2.web.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseDto(
        @NotBlank String code,
        @NotBlank String title,
        String term,
        String description
) {}
