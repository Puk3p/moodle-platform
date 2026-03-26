package moodlev2.web.course.dto.teacher;

import java.util.List;

public record TeacherDashboardResponse(
        List<TeacherCourseDto> courses, List<TeacherActivityDto> recentActivities) {}
