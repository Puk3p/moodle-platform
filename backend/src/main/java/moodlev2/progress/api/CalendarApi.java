package moodlev2.progress.api;

import java.util.Collection;
import java.util.List;

/** Read access to course calendars, which the progress module owns. */
public interface CalendarApi {

    /** Every entry of these courses, in no particular order. */
    List<CalendarEntry> entriesOfCourses(Collection<Long> courseIds);

    /** The course's first {@code limit} entries by date, earliest first. */
    List<CalendarEntry> firstEntriesOfCourse(long courseId, int limit);
}
