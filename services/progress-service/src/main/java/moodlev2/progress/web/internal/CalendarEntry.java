package moodlev2.progress.web.internal;

import java.time.LocalDate;

/** A dated entry on a course's calendar, such as an assignment deadline. */
public record CalendarEntry(
        long id, long courseId, String title, LocalDate date, String type, String description) {}
