package moodlev2.academic.web.course.dto.teacher;

import java.util.List;

public record TeacherDashboardResponse(
        List<TeacherCourseDto> courses, List<TeacherActivityDto> recentActivities) {}
