package moodlev2.assessment.infrastructure.persistence;

import java.util.List;
import moodlev2.assessment.domain.question.QuestionDifficulty;
import moodlev2.assessment.infrastructure.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    List<QuestionEntity> findByCategoryIdAndDifficulty(
            Long categoryId, QuestionDifficulty difficulty);
}
