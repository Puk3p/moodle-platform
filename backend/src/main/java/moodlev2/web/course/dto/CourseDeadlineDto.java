package moodlev2.web.course.dto;

public record CourseDeadlineDto(
        String title,
        String context,
        String due,
        String type
) {}