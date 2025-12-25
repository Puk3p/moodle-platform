package moodlev2.web.course.dto.edit;

import java.util.List;

public record CourseEditDto(
        Long id,
        String code,
        String title,
        String term,
        String status,
        String description,
        List<ModuleEditDto> modules,
        List<Long> selectedGroupIds
) {}