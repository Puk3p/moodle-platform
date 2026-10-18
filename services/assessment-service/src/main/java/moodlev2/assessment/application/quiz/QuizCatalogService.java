package moodlev2.assessment.application.quiz;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.api.QuizCatalogApi;
import moodlev2.assessment.infrastructure.persistence.QuizAttemptRepository;
import moodlev2.assessment.infrastructure.persistence.QuizRepository;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizCatalogService implements QuizCatalogApi {

    private final QuizRepository quizzes;
    private final QuizAttemptRepository attempts;

    @Override
    public List<CourseQuiz> publishedQuizzes(long courseId, Long viewerId) {
        return quizzes.findByCourseIdOrderByIdAsc(courseId).stream()
                .filter(QuizEntity::isPublished)
                .map(q -> toCourseQuiz(q, viewerId))
                .toList();
    }

    private CourseQuiz toCourseQuiz(QuizEntity quiz, Long viewerId) {
        boolean canAttempt = true;
        if (viewerId != null && quiz.getMaxAttempts() != null && quiz.getMaxAttempts() > 0) {
            int used = attempts.countByQuizIdAndUserId(quiz.getId(), viewerId);
            canAttempt = used < quiz.getMaxAttempts();
        }
        return new CourseQuiz(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getModuleId(),
                canAttempt,
                quiz.getQuestionsCount() != null ? quiz.getQuestionsCount() : 0,
                quiz.getDurationMinutes(),
                quiz.getPassword() != null && !quiz.getPassword().isEmpty());
    }
}
