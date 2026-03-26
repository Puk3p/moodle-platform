package moodlev2.web.quiz.dto;

import java.util.List;

public record QuizSubmissionDto(Long attemptId, List<AnswerDto> answers) {
    public record AnswerDto(
            Long questionId,
            Long selectedOptionId,
            String textAnswer,
            List<Long> orderedOptionIds) {}
}
