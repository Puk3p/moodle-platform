package moodlev2.platform.clients;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import moodlev2.academic.application.course.CourseCalendar;
import moodlev2.identity.infrastructure.security.InternalTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link CourseCalendar} served by progress-service's internal API. Core signs its own service
 * token (it is the token issuer). Calls are short and never fail a page: if progress-service is
 * slow or down, the dashboard and course preview simply show no deadlines.
 */
@Component
public class ProgressServiceCalendar implements CourseCalendar {

    private static final Logger log = LoggerFactory.getLogger(ProgressServiceCalendar.class);
    private static final ParameterizedTypeReference<List<Entry>> ENTRIES =
            new ParameterizedTypeReference<>() {};

    private final RestClient progress;
    private final InternalTokens tokens;
    private volatile String token;
    private volatile Instant tokenRenewal = Instant.MIN;

    public ProgressServiceCalendar(
            RestClient.Builder builder,
            InternalTokens tokens,
            @Value("${app.services.progress.url:http://localhost:8085}") String url,
            @Value("${app.services.progress.timeout:1500ms}") Duration timeout) {
        this.tokens = tokens;
        this.progress =
                builder.baseUrl(url)
                        .requestFactory(
                                ClientHttpRequestFactoryBuilder.detect()
                                        .build(
                                                ClientHttpRequestFactorySettings.defaults()
                                                        .withConnectTimeout(timeout)
                                                        .withReadTimeout(timeout)))
                        .build();
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
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                            .body(courseIds)
                            .retrieve()
                            .body(ENTRIES);
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
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
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                            .retrieve()
                            .body(ENTRIES);
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            log.warn("Calendar unavailable, showing none: {}", e.toString());
            return List.of();
        }
    }

    /** Service tokens live five minutes; renew a minute early. */
    private String token() {
        if (Instant.now().isAfter(tokenRenewal)) {
            token = tokens.forService("core");
            tokenRenewal =
                    Instant.now().plus(InternalTokens.SERVICE_TOKEN_LIFETIME).minusSeconds(60);
        }
        return token;
    }
}
