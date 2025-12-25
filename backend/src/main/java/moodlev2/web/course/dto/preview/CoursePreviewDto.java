package moodlev2.web.course.dto.preview;

import java.util.List;

public record CoursePreviewDto(
        String courseCode,
        String title,
        String term,
        String instructorName,
        List<ModulePreviewDto> modules,
        List<AnnouncementPreviewDto> announcements,
        List<DeadlinePreviewDto> deadlines
) {}