package moodlev2.academic.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import moodlev2.academic.infrastructure.persistence.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, Long> {

    List<AnnouncementEntity> findAllByCourseIdInOrderByCreatedAtDesc(Collection<Long> courseIds);

    List<AnnouncementEntity> findTop3ByCourseCodeOrderByCreatedAtDesc(String courseCode);

    List<AnnouncementEntity> findTop5ByOrderByCreatedAtDesc();
}
