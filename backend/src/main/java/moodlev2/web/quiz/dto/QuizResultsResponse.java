package moodlev2.web.quiz.dto;

import java.util.List;

public record QuizResultsResponse(
        Long quizId,
        String quizTitle,
        String courseName,
        String dueDate,
        int timeLimitMinutes,
        boolean isPublished,
        List<QuizAttemptListDto> attempts
) {}