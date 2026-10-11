package moodlev2.progress.web.internal;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.application.calendar.CalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Course calendars for other services (core's dashboard and course preview). */
@RestController
@RequestMapping("/internal/calendar")
@RequiredArgsConstructor
public class CalendarInternalController {

    private final CalendarService calendar;

    @PostMapping("/courses")
    public List<CalendarEntry> entriesOfCourses(@RequestBody List<Long> courseIds) {
        return calendar.entriesOfCourses(courseIds);
    }

    @GetMapping("/courses/{courseId}/first")
    public List<CalendarEntry> firstEntriesOfCourse(
            @PathVariable long courseId, @RequestParam(defaultValue = "3") int limit) {
        return calendar.firstEntriesOfCourse(courseId, Math.min(Math.max(limit, 1), 50));
    }
}
