package moodlev2.platform.clients;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import moodlev2.academic.application.course.CourseQuizzes;
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
 * {@link CourseQuizzes} served by assessment-service's internal API, with a service token core
 * signs itself. If assessment-service is slow or down, course pages list no quizzes rather than
 * failing.
 */
@Component
public class AssessmentServiceQuizzes implements CourseQuizzes {

    private static final Logger log = LoggerFactory.getLogger(AssessmentServiceQuizzes.class);
    private static final ParameterizedTypeReference<List<Quiz>> QUIZZES =
            new ParameterizedTypeReference<>() {};

    private final RestClient assessment;
    private final InternalTokens tokens;
    private volatile String token;
    private volatile Instant tokenRenewal = Instant.MIN;

    public AssessmentServiceQuizzes(
            RestClient.Builder builder,
            InternalTokens tokens,
            @Value("${app.services.assessment.url:http://localhost:8084}") String url,
            @Value("${app.services.assessment.timeout:1500ms}") Duration timeout) {
        this.tokens = tokens;
        this.assessment =
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
    public List<Quiz> publishedQuizzes(long courseId, Long viewerId) {
        try {
            List<Quiz> body =
                    assessment
                            .get()
                            .uri(
                                    b ->
                                            b.path("/internal/quizzes/courses/{id}")
                                                    .queryParamIfPresent(
                                                            "viewerId",
                                                            java.util.Optional.ofNullable(viewerId))
                                                    .build(courseId))
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                            .retrieve()
                            .body(QUIZZES);
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            log.warn("Quizzes unavailable, listing none: {}", e.toString());
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
