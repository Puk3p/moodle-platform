package moodlev2.web.course.dto.teacher;

public record TeacherActivityDto(
        String type,
        String courseCode,
        String title,
        String subtitle,
        String timeAgo,
        String icon
) {}