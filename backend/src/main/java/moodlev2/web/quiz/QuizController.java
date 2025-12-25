// moodlev2\web\quiz\QuizController.java
package moodlev2.web.quiz;

import lombok.RequiredArgsConstructor;
import moodlev2.application.quiz.QuizEngineService;
import moodlev2.application.quiz.QuizManagementService;
import moodlev2.web.quiz.dto.CreateQuizDto;
import moodlev2.web.quiz.dto.QuizResultDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto;
import moodlev2.web.quiz.dto.StudentQuizViewDto;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizManagementService managementService;
    private final QuizEngineService engineService;


    @PostMapping("/create")
    public void createQuiz(@RequestBody CreateQuizDto dto) {
        managementService.createQuiz(dto);
    }


    @PostMapping("/{quizId}/start")
    public StudentQuizViewDto startQuiz(@PathVariable Long quizId, Authentication auth) {
        return engineService.startAttempt(quizId, auth.getName());
    }

    @PostMapping("/submit")
    public QuizResultDto submitQuiz(@RequestBody QuizSubmissionDto dto, Authentication auth) {
        return engineService.submitAttempt(dto, auth.getName());
    }
}