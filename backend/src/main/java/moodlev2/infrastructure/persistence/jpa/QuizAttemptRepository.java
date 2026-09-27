package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    List<QuizAttemptEntity> findByUserEmail(String email);

    Optional<QuizAttemptEntity> findTopByUserEmailAndQuizIdOrderByStartedAtDesc(
            String email, Long quizId);

    List<QuizAttemptEntity> findByQuizIdOrderByCompletedAtDesc(Long quizId);

    int countByQuizIdAndUserId(Long quizId, Long studentId);

    Optional<QuizAttemptEntity> findByQuizIdAndUserIdAndStatus(
            Long quizId, Long userId, String status);

    /** Unsubmitted attempts by this user on quizzes that block messaging while they run. */
    @Query(
            "SELECT a FROM QuizAttemptEntity a JOIN FETCH a.quiz q "
                    + "WHERE a.user.email = :email AND a.status = 'IN_PROGRESS' "
                    + "AND q.blockMessaging = true")
    List<QuizAttemptEntity> findOpenMessagingBlockingAttempts(@Param("email") String email);
}
