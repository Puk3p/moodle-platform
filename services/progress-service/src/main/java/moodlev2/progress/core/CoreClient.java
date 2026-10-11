package moodlev2.progress.core;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Core's internal API over HTTP: client-credentials token (see {@link CoreClientConfig}), short
 * timeouts, one retry for these read-only calls, and the {@code core} circuit breaker.
 */
@Component
@CircuitBreaker(name = "core")
@Retry(name = "core")
public class CoreClient implements CoreDirectory, CoreCourses {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Course>> COURSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient core;

    public CoreClient(RestClient coreRestClient) {
        this.core = coreRestClient;
    }

    @Override
    public Optional<Person> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(
                    core.get()
                            .uri(
                                    b ->
                                            b.path("/internal/directory/users/by-email")
                                                    .queryParam("email", email)
                                                    .build())
                            .retrieve()
                            .body(Person.class));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public List<Person> findUsers(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/directory/users/by-ids", ids, PEOPLE);
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
        List<T> body = core.get().uri(path, id).retrieve().body(type);
        return body == null ? List.of() : body;
    }

    private <T> List<T> post(
            String path, Object request, ParameterizedTypeReference<List<T>> type) {
        List<T> body = core.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
