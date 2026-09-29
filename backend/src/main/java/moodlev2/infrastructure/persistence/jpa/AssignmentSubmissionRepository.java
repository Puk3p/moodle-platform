package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.AssignmentSubmissionEntity;
import moodlev2.infrastructure.persistence.jpa.entity.ModuleItemEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmissionEntity, Long> {
    Optional<AssignmentSubmissionEntity> findByAssignmentAndStudent(
            ModuleItemEntity assignment, UserEntity student);

    List<AssignmentSubmissionEntity> findByAssignmentId(Long assignmentId);

    /**
     * file_url values of this student's submissions that contain the given URL. A submission keeps
     * several files in one column joined by ';', so this is only a pre-filter: callers must split
     * the value and compare each part exactly. LOCATE rather than LIKE so characters such as '_'
     * and '%' in a file name are not treated as wildcards.
     */
    @Query(
            "SELECT s.fileUrl FROM AssignmentSubmissionEntity s "
                    + "WHERE s.student.id = :studentId AND LOCATE(:url, s.fileUrl) > 0")
    List<String> findFileUrlsOfStudentContaining(
            @Param("studentId") Long studentId, @Param("url") String url);
}
