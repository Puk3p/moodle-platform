package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.QuizEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {

    @Query("SELECT q FROM QuizEntity q ORDER BY q.updatedAt DESC")
    List<QuizEntity> findAllQuizzes();
}