package moodlev2.web.course.dto.preview;

public record ModuleItemPreviewDto(
        Long id,
        String title,
        String type,
        String meta,
        boolean isAssignment,
        String url,
        boolean hasPassword
) {}