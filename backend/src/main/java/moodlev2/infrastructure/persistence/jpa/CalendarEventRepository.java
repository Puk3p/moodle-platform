package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {
    @Query("SELECT ce FROM CalendarEventEntity ce JOIN EnrollmentEntity e ON ce.course.id = e.course.id WHERE e.user.email = :email")
    List<CalendarEventEntity> findAllByUserEmail(String email);
}