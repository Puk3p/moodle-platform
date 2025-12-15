package moodlev2.web.course.dto;

import java.util.List;

public record CoursesPageResponse(
        String userName,
        String userRole,
        String userAvatarUrl,
        List<CourseOverviewDto> activeCourses,
        List<CompletedCourseSummaryDto> completedCourses
) {}