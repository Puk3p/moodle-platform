package moodlev2.web.quiz.dto;

import java.math.BigDecimal;

/**
 * @param passed null while {@code awaitingGrading}: the score so far leaves out the answers the
 *     teacher still has to grade, so it cannot decide a pass or a fail
 * @param awaitingGrading the quiz has written answers (free text, code) that only a teacher can
 *     grade
 */
public record QuizResultDto(
        Long attemptId,
        String quizTitle,
        BigDecimal score,
        BigDecimal maxScore,
        Boolean passed,
        boolean awaitingGrading,
        String completedAt) {}
