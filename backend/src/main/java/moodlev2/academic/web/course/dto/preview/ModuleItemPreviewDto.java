package moodlev2.academic.web.course.dto.preview;

public record ModuleItemPreviewDto(
        Long id,
        String title,
        String type,
        String meta,
        boolean isAssignment,
        String url,
        boolean hasPassword) {}
