package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {
    /**
     * Events of every course the user teaches or belongs to (enrolled directly, or through a class
     * the course is assigned to). Membership is tested with EXISTS: navigating {@code e.user.email}
     * off a LEFT JOINed enrollment made Hibernate add an inner join, which dropped every course
     * without a direct enrollment, so class-only members saw none of its deadlines.
     */
    @Query(
            "SELECT ce FROM CalendarEventEntity ce "
                    + "JOIN ce.course c "
                    + "LEFT JOIN c.teacher t "
                    + "WHERE t.email = :email "
                    + "OR EXISTS (SELECT e.id FROM EnrollmentEntity e "
                    + "WHERE e.course = c AND e.user.email = :email) "
                    + "OR EXISTS (SELECT u.id FROM UserEntity u JOIN u.clazz cl "
                    + "WHERE u.email = :email AND cl MEMBER OF c.assignedClasses)")
    List<CalendarEventEntity> findAllByUserEmail(@Param("email") String email);

    List<CalendarEventEntity> findTop3ByCourseCodeOrderByEventDateAsc(String courseCode);
}
