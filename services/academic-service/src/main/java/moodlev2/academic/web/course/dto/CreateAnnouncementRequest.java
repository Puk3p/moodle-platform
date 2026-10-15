package moodlev2.academic.web.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAnnouncementRequest(
        @NotNull Long courseId, @NotBlank String title, @NotBlank String body) {}
