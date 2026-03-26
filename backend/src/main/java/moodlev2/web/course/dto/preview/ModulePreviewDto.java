package moodlev2.web.course.dto.preview;

import java.util.List;

public record ModulePreviewDto(
        Long id,
        String title,
        String dateRange,
        String status,
        String unlockDate,
        List<ModuleItemPreviewDto> items) {}
