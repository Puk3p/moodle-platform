package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {
    @Query("SELECT DISTINCT ce FROM CalendarEventEntity ce " +
            "LEFT JOIN ce.course c " +
            "LEFT JOIN c.enrollments e " +
            "LEFT JOIN c.assignedClasses cl " +
            "WHERE e.user.email = :email " +
            "OR cl.id = (SELECT u.clazz.id FROM UserEntity u WHERE u.email = :email)")
    List<CalendarEventEntity> findAllByUserEmail(@Param("email") String email);

    List<CalendarEventEntity> findTop3ByCourseCodeOrderByEventDateAsc(String courseCode);
}