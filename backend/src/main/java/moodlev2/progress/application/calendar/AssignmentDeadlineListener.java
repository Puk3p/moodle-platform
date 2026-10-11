package moodlev2.progress.application.calendar;

import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.AssignmentPublishedEvent;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Puts an assignment's deadline on its course's calendar. Synchronous, inside the transaction that
 * creates the assignment, so both are saved or neither is.
 */
@Component
@RequiredArgsConstructor
class AssignmentDeadlineListener {

    private final CalendarEventRepository events;

    @EventListener
    void on(AssignmentPublishedEvent assignment) {
        if (assignment.dueDate() == null) {
            return;
        }
        CalendarEventEntity event = new CalendarEventEntity();
        event.setCourseId(assignment.courseId());
        event.setTitle(assignment.title() + " (Deadline)");
        event.setEventDate(assignment.dueDate().toLocalDate());
        event.setEventType("assignment");
        event.setDescription("Deadline for assignment: " + assignment.title());
        events.save(event);
    }
}
