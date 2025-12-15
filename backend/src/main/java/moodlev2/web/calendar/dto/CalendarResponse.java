package moodlev2.web.calendar.dto;

import java.util.List;

public record CalendarResponse(
        List<CalendarEventDto> events
) {}