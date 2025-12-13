package moodlev2.web.course.dto;

public record CompletedCourseDto(
        String code,
        String title,
        String completedAt
) {}
