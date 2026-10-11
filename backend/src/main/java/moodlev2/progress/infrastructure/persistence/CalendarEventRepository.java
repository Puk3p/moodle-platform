package moodlev2.progress.infrastructure.persistence;

import java.util.List;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {
    /**
     * Events of every course the user teaches or belongs to (enrolled directly, or through their
     * class, {@code null} when they have none, being assigned to the course). Membership is tested
     * with EXISTS so a course without any direct enrollment still matches its class members.
     */
    @Query(
            "SELECT ce FROM CalendarEventEntity ce "
                    + "JOIN ce.course c "
                    + "WHERE c.teacherId = :userId "
                    + "OR EXISTS (SELECT e.id FROM EnrollmentEntity e "
                    + "WHERE e.course = c AND e.userId = :userId) "
                    + "OR (:classId IS NOT NULL AND :classId MEMBER OF c.classIds)")
    List<CalendarEventEntity> findAllForUser(
            @Param("userId") Long userId, @Param("classId") Long classId);

    List<CalendarEventEntity> findTop3ByCourseCodeOrderByEventDateAsc(String courseCode);
}
