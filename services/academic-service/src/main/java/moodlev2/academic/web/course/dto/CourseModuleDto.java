package moodlev2.academic.web.course.dto;

import java.util.List;

public record CourseModuleDto(
        String title, String description, List<CourseModuleItemDto> items, String status) {}
