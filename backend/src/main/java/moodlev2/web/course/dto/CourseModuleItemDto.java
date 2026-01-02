package moodlev2.web.course.dto;

public record CourseModuleItemDto(
        Long id,
        String type,
        String label,
        String url,
        Boolean isAssignment,
        Boolean canAttempt
) {}