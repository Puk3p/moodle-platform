package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GradeRepository extends JpaRepository<GradeEntity, Long> {

    @Query("SELECT g FROM GradeEntity g WHERE g.user.email = :email ORDER BY g.gradedAt DESC")
    List<GradeEntity> findAllByUserEmail(String email);

    List<GradeEntity> findByUserEmail(String email);
}
