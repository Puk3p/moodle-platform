package moodlev2.web.course.dto;

public record CourseCurrentModuleDto(
        String title,
        int progress,
        String dueLabel
) {}