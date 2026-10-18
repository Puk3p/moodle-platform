package moodlev2.messaging.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * academic-service's membership API over HTTP. Every call carries a client-credentials token (see
 * {@code ClientsConfig}), times out quickly, is retried once on failure (all calls only read) and
 * goes through the {@code academic} circuit breaker, so an unhealthy service is not hammered and
 * callers fail fast with 503 instead of piling up threads. Accounts come from identity-service
 * ({@link IdentityClient}), quiz locks from assessment-service ({@link AssessmentClient}).
 */
@Component
@CircuitBreaker(name = "academic")
@Retry(name = "academic")
public class AcademicClient implements Membership {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Course>> COURSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient academic;

    public AcademicClient(RestClient academicRestClient) {
        this.academic = academicRestClient;
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

    private <T> List<T> getList(String path, long id, ParameterizedTypeReference<List<T>> type) {
        List<T> body = academic.get().uri(path, id).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
