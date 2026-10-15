package moodlev2.academic.infrastructure.persistence;

import java.util.List;
import moodlev2.academic.infrastructure.persistence.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    List<EnrollmentEntity> findAllByCourseCode(String courseCode);
}
