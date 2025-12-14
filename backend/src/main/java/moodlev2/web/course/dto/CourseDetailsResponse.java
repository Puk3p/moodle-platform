package moodlev2.web.course.dto;

import java.util.List;

public record CourseDetailsResponse(
        String courseCode,
        String fullTitle,
        String termLabel,
        String instructor,
        CourseCurrentModuleDto currentModule,
        CourseStatsDto stats,
        List<CourseModuleDto> modules,
        List<CourseDeadlineDto> deadlines,
        List<CourseAnnouncementDto> announcements
) {}