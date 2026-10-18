package moodlev2.progress.infrastructure.persistence;

import java.util.List;
import moodlev2.progress.infrastructure.persistence.entity.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface GradeRepository extends JpaRepository<GradeEntity, Long> {

    List<GradeEntity> findAllByUserIdOrderByGradedAtDesc(Long userId);

    @Modifying
    @Transactional
    long deleteByUserId(Long userId);
}
