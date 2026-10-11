package moodlev2.progress.infrastructure.persistence;

import java.util.List;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<GradeEntity, Long> {

    List<GradeEntity> findAllByUserIdOrderByGradedAtDesc(Long userId);
}
