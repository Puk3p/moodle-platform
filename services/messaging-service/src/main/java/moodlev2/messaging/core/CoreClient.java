package moodlev2.messaging.core;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * The internal APIs of core (membership) and assessment-service (quiz locks) over HTTP. Every call
 * carries a client-credentials token (see {@code CoreClientConfig}), times out quickly, is retried
 * once on failure (all calls only read) and goes through the {@code core} circuit breaker, so an
 * unhealthy core is not hammered and callers fail fast with 503 instead of piling up threads.
 * Accounts come from identity-service instead ({@link IdentityClient}).
 */
@Component
@CircuitBreaker(name = "core")
@Retry(name = "core")
public class CoreClient implements CoreMembership, CoreExamLocks {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Course>> COURSES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OpenExam>> EXAMS =
            new ParameterizedTypeReference<>() {};

    private final RestClient core;
    private final RestClient assessment;

    public CoreClient(RestClient coreRestClient, RestClient assessmentRestClient) {
        this.core = coreRestClient;
        this.assessment = assessmentRestClient;
    }

    @Override
    public List<Course> coursesOfStudent(long userId) {
        return getList("/internal/membership/students/{id}/courses", userId, COURSES);
    }

    @Override
    public List<Course> coursesTaughtBy(long teacherId) {
        return getList("/internal/membership/teachers/{id}/courses", teacherId, COURSES);
    }

    @Override
    public List<Person> studentsOf(long courseId) {
        return getList("/internal/membership/courses/{id}/students", courseId, PEOPLE);
    }

    @Override
    public List<OpenExam> openMessagingBlockingAttempts(long userId) {
        // Quizzes live in assessment-service; same token, same resilience.
        List<OpenExam> body =
                assessment
                        .get()
                        .uri("/internal/exam-locks/users/{id}", userId)
                        .retrieve()
                        .body(EXAMS);
        return body == null ? List.of() : body;
    }

    private <T> List<T> getList(String path, long id, ParameterizedTypeReference<List<T>> type) {
        List<T> body = core.get().uri(path, id).retrieve().body(type);
        return body == null ? List.of() : body;
    }

    private <T> List<T> postList(
            String path, Object request, ParameterizedTypeReference<List<T>> type) {
        List<T> body = core.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
