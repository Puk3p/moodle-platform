package moodlev2.web.course.dto;

import java.util.List;

public record DashboardHomeResponse(
        String userName,
        List<ActiveCourseDto> activeCourses,
        List<CompletedCourseDto> completedCourses,
        List<DeadlineDto> deadlines,
        List<ActivityDto> activities) {}
