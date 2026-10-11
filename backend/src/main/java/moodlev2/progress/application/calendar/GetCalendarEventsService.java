package moodlev2.progress.application.calendar;

import java.util.List;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.infrastructure.persistence.entity.CalendarEventEntity;
import moodlev2.progress.web.calendar.dto.CalendarEventDto;
import moodlev2.progress.web.calendar.dto.CalendarResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCalendarEventsService {

    private final CalendarEventRepository calendarRepository;
    private final DirectoryApi directory;

    public CalendarResponse getCalendarEventsForUser(String email) {
        UserSummary user = email == null ? null : directory.findUserByEmail(email).orElse(null);
        if (user == null) {
            return new CalendarResponse(List.of());
        }

        List<CalendarEventEntity> userEvents =
                calendarRepository.findAllForUser(user.id(), user.classId());

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
