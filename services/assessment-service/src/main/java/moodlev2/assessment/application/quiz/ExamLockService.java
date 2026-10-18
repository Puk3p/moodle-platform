package moodlev2.assessment.application.quiz;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.api.ExamLockApi;
import moodlev2.assessment.infrastructure.persistence.QuizAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamLockService implements ExamLockApi {

    private final QuizAttemptRepository attempts;

    @Override
    public List<OpenExam> openMessagingBlockingAttempts(long userId) {
        return attempts.findOpenMessagingBlockingAttempts(userId).stream()
                .map(a -> new OpenExam(a.getStartedAt(), a.getQuiz().getDurationMinutes()))
                .toList();
    }
}
