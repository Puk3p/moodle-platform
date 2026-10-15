package moodlev2.assessment.web.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.api.ExamLockApi;
import moodlev2.assessment.api.ExamLockApi.OpenExam;
import moodlev2.assessment.api.QuizCatalogApi;
import moodlev2.assessment.api.QuizCatalogApi.CourseQuiz;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * For other services: exam locks (messaging-service blocks chat during a quiz) and a course's
 * quizzes (core's course page and preview).
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class AssessmentInternalController {

    private final ExamLockApi examLocks;
    private final QuizCatalogApi quizCatalog;

    @GetMapping("/exam-locks/users/{userId}")
    public List<OpenExam> openMessagingBlockingAttempts(@PathVariable long userId) {
        return examLocks.openMessagingBlockingAttempts(userId);
    }

    @GetMapping("/quizzes/courses/{courseId}")
    public List<CourseQuiz> publishedQuizzes(
            @PathVariable long courseId, @RequestParam(required = false) Long viewerId) {
        return quizCatalog.publishedQuizzes(courseId, viewerId);
    }
}
