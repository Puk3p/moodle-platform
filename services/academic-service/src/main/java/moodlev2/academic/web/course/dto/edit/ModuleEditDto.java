package moodlev2.academic.web.course.dto.edit;

public record ModuleEditDto(
        Long id,
        String title,
        String description,
        int sortOrder,
        String startDate,
        String endDate,
        String status,
        ModuleStatsDto stats) {}
