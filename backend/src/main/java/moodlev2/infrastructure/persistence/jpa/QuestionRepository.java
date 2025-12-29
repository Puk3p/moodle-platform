package moodlev2.infrastructure.persistence.jpa;

import moodlev2.domain.question.QuestionDifficulty;
import moodlev2.infrastructure.persistence.jpa.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findByCategoryId(Long categoryId);


    long countByCategoryId(Long categoryId);

    List<QuestionEntity> findByCategoryIdAndDifficulty(Long categoryId, QuestionDifficulty difficulty);
}
