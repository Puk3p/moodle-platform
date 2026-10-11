package moodlev2.academic.infrastructure.persistence;

import java.util.List;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {

    /** Announcements of courses the user is enrolled in directly, newest first. */
    @Query(
            "SELECT a FROM AnnouncementEntity a JOIN EnrollmentEntity e ON a.course.id = e.course.id "
                    + "WHERE e.userId = :userId ORDER BY a.createdAt DESC")
    List<AnnouncementEntity> findAllForEnrolledUser(@Param("userId") Long userId);

    List<AnnouncementEntity> findTop3ByCourseCodeOrderByCreatedAtDesc(String courseCode);

    List<AnnouncementEntity> findTop5ByOrderByCreatedAtDesc();
}
