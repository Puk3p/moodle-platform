package moodlev2.web.course.dto.edit;

public record ModuleEditDto(
        Long id,
        String title,
        String description,
        int sortOrder,
        String startDate,
        String endDate,
        String status,
        ModuleStatsDto stats
) {}