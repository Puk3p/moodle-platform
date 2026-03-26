package moodlev2.application.calendar;

import java.util.List;
import moodlev2.web.calendar.dto.CalendarEventDto;
import moodlev2.web.calendar.dto.CalendarResponse;
import org.springframework.stereotype.Service;

@Service
public class GetCalendarEventsService {

    public CalendarResponse getCalendarEventsForUser(String email) {

        List<CalendarEventDto> events =
                List.of(
                        new CalendarEventDto(1L, "2024-10-09", "Lab 4: Trees", "CS201", "lab"),
                        new CalendarEventDto(
                                2L, "2024-10-09", "Project Proposal", "CS350", "project"),
                        new CalendarEventDto(
                                3L, "2024-10-17", "Assignment 3", "CS201", "assignment"),
                        new CalendarEventDto(4L, "2024-10-23", "Quiz 2", "CS201", "quiz"),
                        new CalendarEventDto(5L, "2024-10-30", "Midterm Exam", "CS201", "quiz"));

        return new CalendarResponse(events);
    }
}
