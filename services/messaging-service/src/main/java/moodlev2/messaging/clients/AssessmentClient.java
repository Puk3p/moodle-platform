package moodlev2.messaging.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * assessment-service's quiz-lock API over HTTP: client-credentials token (see {@code
 * ClientsConfig}), short timeouts, one retry, and the {@code assessment} circuit breaker.
 */
@Component
@CircuitBreaker(name = "assessment")
@Retry(name = "assessment")
public class AssessmentClient implements ExamLocks {

    private static final ParameterizedTypeReference<List<OpenExam>> EXAMS =
            new ParameterizedTypeReference<>() {};

    private final RestClient assessment;

    public AssessmentClient(RestClient assessmentRestClient) {
        this.assessment = assessmentRestClient;
    }

    @Override
    public List<OpenExam> openMessagingBlockingAttempts(long userId) {
        List<OpenExam> body =
                assessment
                        .get()
                        .uri("/internal/exam-locks/users/{id}", userId)
                        .retrieve()
                        .body(EXAMS);
        return body == null ? List.of() : body;
    }
}
