package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {

    @Query("SELECT a FROM AnnouncementEntity a JOIN EnrollmentEntity e ON a.course.id = e.course.id WHERE e.user.email = :email ORDER BY a.createdAt DESC")
    List<AnnouncementEntity> findAllByUserEmail(String email);

    List<AnnouncementEntity> findTop3ByCourseCodeOrderByCreatedAtDesc(String courseCode);

    List<AnnouncementEntity> findTop5ByOrderByCreatedAtDesc();
}