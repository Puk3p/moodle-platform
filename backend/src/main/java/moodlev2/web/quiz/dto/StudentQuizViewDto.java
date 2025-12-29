package moodlev2.web.quiz.dto;

import java.util.List;

public record StudentQuizViewDto(
        Long quizId,
        String title,
        Integer timeLimitMinutes,
        List<StudentQuestionDto> questions
) {
    public record StudentQuestionDto(
            Long id,
            String text,
            Integer points,
            List<StudentOptionDto> options
    ) {}

    public record StudentOptionDto(
            Long id,
            String text
    ) {}
}