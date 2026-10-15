package moodlev2.academic.application.course;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Course calendars (deadlines), kept by progress-service. Screens here only show them, so a
 * calendar that cannot be reached means an empty list, never a failed page.
 */
public interface CourseCalendar {

    /** Every entry of these courses, in no particular order; empty if unavailable. */
    List<Entry> entriesOfCourses(Collection<Long> courseIds);

    /** The course's first {@code limit} entries by date; empty if unavailable. */
    List<Entry> firstEntriesOfCourse(long courseId, int limit);

    record Entry(long id, long courseId, String title, LocalDate date, String type) {}
}
