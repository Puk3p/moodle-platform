package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    List<QuizAttemptEntity> findByUserEmail(String email);

    Optional<QuizAttemptEntity> findTopByUserEmailAndQuizIdOrderByStartedAtDesc(
            String email, Long quizId);

    List<QuizAttemptEntity> findByQuizIdOrderByCompletedAtDesc(Long quizId);

    int countByQuizIdAndUserId(Long quizId, Long studentId);

    Optional<QuizAttemptEntity> findByQuizIdAndUserIdAndStatus(
            Long quizId, Long userId, String status);
}
