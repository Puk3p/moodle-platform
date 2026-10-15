package moodlev2.assessment.web.quiz;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.application.quiz.GetTeacherQuizzesService;
import moodlev2.assessment.application.quiz.ProctorService;
import moodlev2.assessment.application.quiz.QuizEngineService;
import moodlev2.assessment.web.quiz.dto.ProctorReportDto;
import moodlev2.assessment.web.quiz.dto.QuizAttemptListDto;
import moodlev2.assessment.web.quiz.dto.QuizAttemptReviewDto;
import moodlev2.assessment.web.quiz.dto.QuizResultsResponse;
import moodlev2.assessment.web.quiz.dto.TeacherQuizDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/quizzes")
@RequiredArgsConstructor
public class TeacherQuizController {

    private final GetTeacherQuizzesService getTeacherQuizzesService;
    private final QuizEngineService quizEngineService;
    private final ProctorService proctorService;

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

    /** Proctoring timeline for one attempt: how often the student left the exam surface. */
    @GetMapping("/attempts/{attemptId}/proctor")
    public ProctorReportDto getProctorReport(@PathVariable Long attemptId) {
        return proctorService.report(attemptId);
    }

    @PatchMapping("/attempts/{attemptId}/questions/{questionId}/score")
    public void updateScore(
            @PathVariable Long attemptId,
            @PathVariable Long questionId,
            @RequestBody BigDecimal score) {
        quizEngineService.updateQuestionScore(attemptId, questionId, score);
    }
}
