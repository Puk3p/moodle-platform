package moodlev2.web.course;

import lombok.RequiredArgsConstructor;
import moodlev2.application.course.GetTeacherQuizzesService;
import moodlev2.application.quiz.QuizEngineService;
import moodlev2.web.course.dto.teacher.TeacherQuizDto;
import moodlev2.web.quiz.dto.QuizAttemptListDto;
import moodlev2.web.quiz.dto.QuizAttemptReviewDto;
import moodlev2.web.quiz.dto.QuizResultsResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/quizzes")
@RequiredArgsConstructor
public class TeacherQuizController {

    private final GetTeacherQuizzesService getTeacherQuizzesService;
    private final QuizEngineService quizEngineService;

    @GetMapping
    public List<TeacherQuizDto> getQuizzes(Authentication authentication) {
        return getTeacherQuizzesService.getQuizzesForTeacher(authentication.getName());
    }

    @GetMapping("/{quizId}/attempts")
    public List<QuizAttemptListDto> getQuizAttempts(@PathVariable Long quizId) {
        return quizEngineService.getAttemptsForQuiz(quizId);
    }

    @GetMapping("/{quizId}/results")
    public QuizResultsResponse getQuizResults(@PathVariable Long quizId) {
        return quizEngineService.getQuizResultsWithMetadata(quizId);
    }

    @GetMapping("/attempts/{attemptId}/review")
    public QuizAttemptReviewDto getAttemptReview(@PathVariable Long attemptId) {
        return quizEngineService.getAttemptReview(attemptId);
    }

    @PatchMapping("/attempts/{attemptId}/questions/{questionId}/score")
    public void updateScore(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody BigDecimal score) {
        quizEngineService.updateQuestionScore(attemptId, questionId, score);
    }
}