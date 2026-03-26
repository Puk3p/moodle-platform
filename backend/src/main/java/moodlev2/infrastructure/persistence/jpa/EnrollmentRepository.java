package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    List<EnrollmentEntity> findAllByCourseCode(String courseCode);
}
