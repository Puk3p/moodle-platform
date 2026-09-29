package moodlev2.web.grade.dto;

import java.util.List;

/**
 * Everything the grades page shows. {@code overallGpa}, {@code bestCourse} and {@code
 * needsAttention} are null when there is nothing to report yet.
 */
public record GradesPageResponse(
        List<CourseGradeDto> courses,
        Double overallGpa,
        GradeBreakdownDto gradeBreakdown,
        SimpleCourseGradeDto bestCourse,
        SimpleCourseGradeDto needsAttention,
        List<UpcomingGradeDto> upcomingGradeReleases) {}
