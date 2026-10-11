package moodlev2.messaging.core;

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
 * Core's internal API over HTTP. Every call carries a client-credentials token (see {@code
 * CoreClientConfig}), times out quickly, is retried once on failure (all calls only read) and goes
 * through the {@code core} circuit breaker, so an unhealthy core is not hammered and callers fail
 * fast with 503 instead of piling up threads.
 */
@Component
@CircuitBreaker(name = "core")
@Retry(name = "core")
public class CoreClient implements CoreDirectory, CoreMembership, CoreExamLocks {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Course>> COURSES =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<OpenExam>> EXAMS =
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
    public List<Person> findUsersByEmail(Collection<String> emails) {
        return emails.isEmpty()
                ? List.of()
                : postList("/internal/directory/users/by-emails", emails, PEOPLE);
    }

    @Override
    public List<Person> findUsers(Collection<Long> ids) {
        return ids.isEmpty()
                ? List.of()
                : postList("/internal/directory/users/by-ids", ids, PEOPLE);
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
        return getList("/internal/exam-locks/users/{id}", userId, EXAMS);
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
