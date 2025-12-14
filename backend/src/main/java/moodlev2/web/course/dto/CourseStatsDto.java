package moodlev2.web.course.dto;

public record CourseStatsDto(
        int overallProgress,
        int completedLabs,
        int totalLabs,
        String averageGrade
) {}