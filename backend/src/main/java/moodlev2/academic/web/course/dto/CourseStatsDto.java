package moodlev2.academic.web.course.dto;

public record CourseStatsDto(
        int overallProgress, int completedLabs, int totalLabs, String averageGrade) {}
