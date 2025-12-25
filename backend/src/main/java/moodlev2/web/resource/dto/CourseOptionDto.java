package moodlev2.web.resource.dto;

import java.util.List;

public record CourseOptionDto(
        String code,
        String name,
        List<ModuleOptionDto> modules
) {}
