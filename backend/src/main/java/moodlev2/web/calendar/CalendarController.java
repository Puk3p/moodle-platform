package moodlev2.web.calendar;

import lombok.RequiredArgsConstructor;
import moodlev2.application.calendar.GetCalendarEventsService;
import moodlev2.web.calendar.dto.CalendarResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final GetCalendarEventsService getCalendarEventsUseCase;

    @GetMapping
    public CalendarResponse getMyCalendar(Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return getCalendarEventsUseCase.getCalendarEventsForUser(email);
    }
}
