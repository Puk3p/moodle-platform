package moodlev2.application.calendar;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.persistence.jpa.CalendarEventRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CalendarEventEntity;
import moodlev2.web.calendar.dto.CalendarEventDto;
import moodlev2.web.calendar.dto.CalendarResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCalendarEventsService {

    private final CalendarEventRepository calendarRepository;

    public CalendarResponse getCalendarEventsForUser(String email) {
        if (email == null) {
            return new CalendarResponse(List.of());
        }

        List<CalendarEventEntity> userEvents = calendarRepository.findAllByUserEmail(email);

        List<CalendarEventDto> events =
                userEvents.stream()
                        .map(
                                e ->
                                        new CalendarEventDto(
                                                e.getId(),
                                                e.getEventDate() != null
                                                        ? e.getEventDate().toString()
                                                        : "",
                                                e.getTitle(),
                                                e.getCourse() != null
                                                        ? e.getCourse().getCode()
                                                        : "",
                                                e.getEventType()))
                        .toList();

        return new CalendarResponse(events);
    }
}
