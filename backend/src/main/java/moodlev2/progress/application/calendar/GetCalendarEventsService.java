package moodlev2.progress.application.calendar;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.api.CourseSummary;
import moodlev2.academic.api.MembershipApi;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.progress.infrastructure.persistence.CalendarEventRepository;
import moodlev2.progress.web.calendar.dto.CalendarEventDto;
import moodlev2.progress.web.calendar.dto.CalendarResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetCalendarEventsService {

    private final CalendarEventRepository calendarRepository;
    private final DirectoryApi directory;
    private final MembershipApi membership;

    /** Entries of every course the user teaches or belongs to. */
    public CalendarResponse getCalendarEventsForUser(String email) {
        UserSummary user = email == null ? null : directory.findUserByEmail(email).orElse(null);
        if (user == null) {
            return new CalendarResponse(List.of());
        }

        Map<Long, String> codes =
                membership.coursesInvolving(user).stream()
                        .collect(Collectors.toMap(CourseSummary::id, CourseSummary::code));

        List<CalendarEventDto> events =
                calendarRepository.findAllByCourseIdIn(codes.keySet()).stream()
                        .map(
                                e ->
                                        new CalendarEventDto(
                                                e.getId(),
                                                e.getEventDate() != null
                                                        ? e.getEventDate().toString()
                                                        : "",
                                                e.getTitle(),
                                                codes.getOrDefault(e.getCourseId(), ""),
                                                e.getEventType()))
                        .toList();

        return new CalendarResponse(events);
    }
}
