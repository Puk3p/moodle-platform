package moodlev2.web.grade.dto;

import java.util.List;

/**
 * One enrolled course on the student's grades page. {@code gradeLetter} and {@code percentage} are
 * null until the course has at least one graded item, so an empty course never reads as F.
 */
public record CourseGradeDto(
        String code,
        String name,
        String term,
        String instructor,
        String gradeLetter,
        Integer percentage,
        int gradedCount,
        List<RecentGradeItemDto> recentItems) {}
