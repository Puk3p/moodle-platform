package moodlev2.web.quiz.dto;

import java.math.BigDecimal;

public record QuizAttemptListDto(
        Long attemptId,
        Long studentId,
        String studentName,
        String studentEmail,
        String studentAvatarColor,
        String groupName,
        String submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        boolean passed) {}
