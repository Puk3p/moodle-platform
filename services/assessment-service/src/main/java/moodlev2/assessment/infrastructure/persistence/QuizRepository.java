package moodlev2.assessment.infrastructure.persistence;

import java.util.List;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    @Query("SELECT q FROM QuizEntity q ORDER BY q.updatedAt DESC")
    List<QuizEntity> findAllQuizzes();

    List<QuizEntity> findByCourseIdOrderByIdAsc(Long courseId);
}
