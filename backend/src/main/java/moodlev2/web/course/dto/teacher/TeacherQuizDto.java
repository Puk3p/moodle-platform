package moodlev2.web.course.dto.teacher;

public record TeacherQuizDto(
        String id,
        String title,
        String courseName,
        String context,
        String status,
        int questionsCount,
        int durationMinutes,
        String attemptsLabel,
        String lastUpdated
) {}