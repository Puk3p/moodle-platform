package moodlev2.web.quiz;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.application.quiz.ProctorService;
import moodlev2.application.quiz.QuizEngineService;
import moodlev2.application.quiz.QuizManagementService;
import moodlev2.web.quiz.dto.CreateQuizDto;
import moodlev2.web.quiz.dto.ProctorEventBatchDto;
import moodlev2.web.quiz.dto.QuizResultDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto;
import moodlev2.web.quiz.dto.StudentQuizViewDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizManagementService managementService;
    private final QuizEngineService engineService;
    private final ProctorService proctorService;

    public record StartQuizRequest(String password) {}

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void createQuiz(@RequestBody CreateQuizDto dto) {
        managementService.createQuiz(dto);
    }

    @PostMapping("/{quizId}/start")
    public StudentQuizViewDto startQuiz(
            @PathVariable Long quizId,
            @RequestBody(required = false) StartQuizRequest request,
            Authentication auth) {

        String password = (request != null) ? request.password() : null;

        return engineService.startAttempt(quizId, auth.getName(), password);
    }

    @PostMapping("/submit")
    public QuizResultDto submitQuiz(@RequestBody QuizSubmissionDto dto, Authentication auth) {
        return engineService.submitAttempt(dto, auth.getName());
    }

    /**
     * Records proctoring signals for the caller's own attempt. Ownership is enforced in the
     * service, so a student cannot post events onto another student's attempt.
     */
    @PostMapping("/attempts/{attemptId}/proctor-events")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordProctorEvents(
            @PathVariable Long attemptId,
            @RequestBody ProctorEventBatchDto batch,
            Authentication auth) {

        List<ProctorService.ProctorInput> inputs =
                batch == null || batch.events() == null
                        ? List.of()
                        : batch.events().stream()
                                .map(
                                        e ->
                                                new ProctorService.ProctorInput(
                                                        e.type(), e.detail(), e.clientTs()))
                                .toList();

        proctorService.record(attemptId, auth.getName(), inputs);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void deleteQuiz(@PathVariable Long id) {
        managementService.deleteQuiz(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public void updateQuiz(@PathVariable Long id, @RequestBody CreateQuizDto dto) {
        managementService.updateQuiz(id, dto);
    }
}
