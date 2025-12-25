package moodlev2.web.course.dto.students;

public record StudentStatsDto(
        long total,
        long activeRate,
        long pending
) {}