package moodlev2.progress.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventRepository extends JpaRepository<CalendarEventEntity, Long> {

    List<CalendarEventEntity> findAllByCourseIdIn(Collection<Long> courseIds);

    List<CalendarEventEntity> findByCourseIdOrderByEventDateAsc(Long courseId, Limit limit);
}
