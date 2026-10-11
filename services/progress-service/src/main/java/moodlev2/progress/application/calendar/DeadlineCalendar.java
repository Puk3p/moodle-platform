package moodlev2.progress.application.calendar;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Files assignment deadlines on their course's calendar. Events can arrive more than once (the
 * outbox delivers at least once), so an entry is keyed by the assignment it comes from and a repeat
 * updates it instead of adding a second one.
 */
@Service
@RequiredArgsConstructor
public class DeadlineCalendar {

    static final String ASSIGNMENT = "assignment";

    private final CalendarEventRepository events;

    @Transactional
    public void assignmentPublished(
            long courseId, long assignmentId, String title, LocalDateTime dueDate) {
        if (dueDate == null) {
            return;
        }
        CalendarEventEntity event =
                events.findBySourceTypeAndSourceId(ASSIGNMENT, assignmentId)
                        .orElseGet(CalendarEventEntity::new);
        event.setCourseId(courseId);
        event.setSourceType(ASSIGNMENT);
        event.setSourceId(assignmentId);
        event.setTitle(title + " (Deadline)");
        event.setEventDate(dueDate.toLocalDate());
        event.setEventType("assignment");
        event.setDescription("Deadline for assignment: " + title);
        events.save(event);
    }
}
