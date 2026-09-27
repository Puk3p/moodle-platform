package moodlev2.web.quiz.dto;

import java.util.List;

public record StudentQuizViewDto(
        Long attemptId,
        Long quizId,
        String title,
        Integer timeLimitMinutes,
        List<StudentQuestionDto> questions,
        // Disclosed to the student up-front, like proctoring: messaging is off until they submit.
        boolean messagingBlocked) {
    public record StudentQuestionDto(
            Long id, String text, Integer points, String type, List<StudentOptionDto> options) {}

    public record StudentOptionDto(Long id, String text) {}
}
