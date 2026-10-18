package moodlev2.progress.application.calendar;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import moodlev2.progress.web.internal.CalendarEntry;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarEventRepository events;

    public List<CalendarEntry> entriesOfCourses(Collection<Long> courseIds) {
        return courseIds.isEmpty()
                ? List.of()
                : events.findAllByCourseIdIn(courseIds).stream()
                        .map(CalendarService::toEntry)
                        .toList();
    }

    public List<CalendarEntry> firstEntriesOfCourse(long courseId, int limit) {
        return events.findByCourseIdOrderByEventDateAsc(courseId, Limit.of(limit)).stream()
                .map(CalendarService::toEntry)
                .toList();
    }

    static CalendarEntry toEntry(CalendarEventEntity e) {
        return new CalendarEntry(
                e.getId(),
                e.getCourseId(),
                e.getTitle(),
                e.getEventDate(),
                e.getEventType(),
                e.getDescription());
    }
}
