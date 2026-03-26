package moodlev2.web.grade.dto;

import java.util.List;

public record GradesPageResponse(
        List<CourseGradeDto> courses,
        double overallGpa,
        double gpaDelta,
        GradeBreakdownDto gradeBreakdown,
        SimpleCourseGradeDto bestCourse,
        SimpleCourseGradeDto needsAttention,
        List<UpcomingGradeDto> upcomingGradeReleases) {}
