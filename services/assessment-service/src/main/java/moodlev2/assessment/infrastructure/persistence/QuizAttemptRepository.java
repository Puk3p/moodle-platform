package moodlev2.assessment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import moodlev2.assessment.infrastructure.persistence.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    List<QuizAttemptEntity> findByQuizIdOrderByCompletedAtDesc(Long quizId);

    int countByQuizIdAndUserId(Long quizId, Long studentId);

    Optional<QuizAttemptEntity> findByQuizIdAndUserIdAndStatus(
            Long quizId, Long userId, String status);

    /** Unsubmitted attempts by this user on quizzes that block messaging while they run. */
    @Query(
            "SELECT a FROM QuizAttemptEntity a JOIN FETCH a.quiz q "
                    + "WHERE a.userId = :userId AND a.status = 'IN_PROGRESS' "
                    + "AND q.blockMessaging = true")
    List<QuizAttemptEntity> findOpenMessagingBlockingAttempts(@Param("userId") Long userId);
}
