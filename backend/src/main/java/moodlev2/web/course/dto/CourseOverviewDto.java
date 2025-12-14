package moodlev2.web.course.dto;

public record CourseOverviewDto(
        String id,
        String code,
        String title,
        String prof,
        int progress,
        String nextDeadline,
        String imageUrl
) {}