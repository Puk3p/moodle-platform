package moodlev2.academic.clients;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link CourseQuizzes} served by assessment-service's internal API (service token and timeouts:
 * see {@link ClientsConfig}). If assessment-service is slow or down, course pages list no quizzes
 * rather than failing.
 */
@Component
public class AssessmentClient implements CourseQuizzes {

    private static final Logger log = LoggerFactory.getLogger(AssessmentClient.class);
    private static final ParameterizedTypeReference<List<Quiz>> QUIZZES =
            new ParameterizedTypeReference<>() {};

    private final RestClient assessment;

    public AssessmentClient(RestClient assessmentRestClient) {
        this.assessment = assessmentRestClient;
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
                            .retrieve()
                            .body(QUIZZES);
            return body == null ? List.of() : body;
        } catch (RestClientException | OAuth2AuthorizationException e) {
            log.warn("Quizzes unavailable, listing none: {}", e.toString());
            return List.of();
        }
    }
}
