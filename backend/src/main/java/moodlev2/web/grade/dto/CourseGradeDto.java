package moodlev2.web.grade.dto;

import java.util.List;

public record CourseGradeDto(
        String code,
        String name,
        String instructor,
        String gradeLetter,
        int percentage,
        String status,
        boolean isCurrent,
        List<RecentGradeItemDto> recentItems) {}
