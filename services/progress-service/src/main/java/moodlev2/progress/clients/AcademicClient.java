package moodlev2.progress.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * academic-service's course API over HTTP: client-credentials token (see {@link ClientsConfig}),
 * short timeouts, one retry for these read-only calls, and the {@code academic} circuit breaker.
 * Accounts come from identity-service instead ({@link IdentityClient}).
 */
@Component
@CircuitBreaker(name = "academic")
@Retry(name = "academic")
public class AcademicClient implements Courses {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Course>> COURSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient academic;

    public AcademicClient(RestClient academicRestClient) {
        this.academic = academicRestClient;
    }

    @Override
    public List<Course> findCourses(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/catalog/courses/by-ids", ids, COURSES);
    }

    @Override
    public List<Course> coursesOfStudent(long userId) {
        return get("/internal/membership/students/{id}/courses", userId, COURSES);
    }

    @Override
    public List<Course> coursesInvolving(long userId) {
        return get("/internal/membership/users/{id}/courses", userId, COURSES);
    }

    private <T> List<T> get(String path, long id, ParameterizedTypeReference<List<T>> type) {
        List<T> body = academic.get().uri(path, id).retrieve().body(type);
        return body == null ? List.of() : body;
    }

    private <T> List<T> post(
            String path, Object request, ParameterizedTypeReference<List<T>> type) {
        List<T> body = academic.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
