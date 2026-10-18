package moodlev2.academic.web.course.dto;

import java.util.List;

public record CoursesPageResponse(
        String userName,
        String userRole,
        String userAvatarUrl,
        List<CourseOverviewDto> activeCourses,
        List<CompletedCourseSummaryDto> completedCourses,
        List<DeadlineDto> deadlines,
        List<ActivityDto> activities) {}
