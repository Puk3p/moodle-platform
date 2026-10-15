package moodlev2.academic.infrastructure.clients;

import java.util.Collection;
import java.util.List;
import moodlev2.academic.application.course.CourseCalendar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link CourseCalendar} served by progress-service's internal API (service token and timeouts: see
 * {@link InternalClientsConfig}). Calls never fail a page: if progress-service is slow or down, the
 * dashboard and course preview simply show no deadlines.
 */
@Component
public class ProgressServiceCalendar implements CourseCalendar {

    private static final Logger log = LoggerFactory.getLogger(ProgressServiceCalendar.class);
    private static final ParameterizedTypeReference<List<Entry>> ENTRIES =
            new ParameterizedTypeReference<>() {};

    private final RestClient progress;

    public ProgressServiceCalendar(RestClient progressRestClient) {
        this.progress = progressRestClient;
    }

    @Override
    public List<Entry> entriesOfCourses(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        try {
            List<Entry> body =
                    progress.post()
                            .uri("/internal/calendar/courses")
                            .body(courseIds)
                            .retrieve()
                            .body(ENTRIES);
            return body == null ? List.of() : body;
        } catch (RestClientException | OAuth2AuthorizationException e) {
            log.warn("Calendar unavailable, showing none: {}", e.toString());
            return List.of();
        }
    }

    @Override
    public List<Entry> firstEntriesOfCourse(long courseId, int limit) {
        try {
            List<Entry> body =
                    progress.get()
                            .uri(
                                    "/internal/calendar/courses/{id}/first?limit={limit}",
                                    courseId,
                                    limit)
                            .retrieve()
                            .body(ENTRIES);
            return body == null ? List.of() : body;
        } catch (RestClientException | OAuth2AuthorizationException e) {
            log.warn("Calendar unavailable, showing none: {}", e.toString());
            return List.of();
        }
    }
}
