package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.AssignmentSubmissionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmissionEntity, Long> {
    Optional<AssignmentSubmissionEntity> findByAssignmentAndStudent(
            ModuleItemEntity assignment, UserEntity student);

    List<AssignmentSubmissionEntity> findByAssignmentId(Long assignmentId);
}
