package moodlev2.web.quiz.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * A finished attempt. The service also refuses more answers than the quiz has questions; the size
 * bound here only stops an absurd payload before it is walked.
 */
public record QuizSubmissionDto(
        @NotNull Long attemptId, @NotNull @Size(max = 500) List<AnswerDto> answers) {
    public record AnswerDto(
            Long questionId,
            Long selectedOptionId,
            String textAnswer,
            List<Long> orderedOptionIds) {}
}
