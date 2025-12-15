package moodlev2.web.resource.dto;

import java.util.List;

public record CourseResourcesDto(
        String courseCode,
        String courseName,
        List<ResourceFileDto> files
) {}