package moodlev2.assessment.web.quiz.dto;

public record TeacherQuizDto(
        String id,
        String title,
        String courseName,
        String context,
        String status,
        int questionsCount,
        int durationMinutes,
        String attemptsLabel,
        String lastUpdated,
        boolean blockMessaging) {}
