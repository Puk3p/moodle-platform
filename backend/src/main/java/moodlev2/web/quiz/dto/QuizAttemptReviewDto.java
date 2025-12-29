package moodlev2.web.quiz.dto;

import java.math.BigDecimal;
import java.util.List;

public record QuizAttemptReviewDto(
        Long attemptId,
        String studentName,
        String groupName,
        String quizTitle,
        BigDecimal finalScore,
        BigDecimal maxScore,
        String timeTaken,
        String submittedAt,
        List<QuestionReviewDto> questions
) {}