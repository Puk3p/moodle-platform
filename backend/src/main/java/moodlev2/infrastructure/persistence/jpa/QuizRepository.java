package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    @Query("SELECT q FROM QuizEntity q ORDER BY q.updatedAt DESC")
    List<QuizEntity> findAllQuizzes();
}
