package moodlev2.assessment.core;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Core's course API over HTTP (membership and catalog): client-credentials token (see {@link
 * CoreClientConfig}), short timeouts, one retry for these read-only calls, and the {@code core}
 * circuit breaker. Accounts come from identity-service instead ({@link IdentityClient}).
 */
@Component
@CircuitBreaker(name = "core")
@Retry(name = "core")
public class CoreClient implements MembershipApi, CourseCatalogApi {

    private static final ParameterizedTypeReference<List<UserSummary>> USERS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CourseSummary>> COURSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient core;

    public CoreClient(RestClient coreRestClient) {
        this.core = coreRestClient;
    }

    @Override
    public boolean isMember(long courseId, UserSummary user) {
        Boolean member =
                core.get()
                        .uri("/internal/membership/courses/{c}/members/{u}", courseId, user.id())
                        .retrieve()
                        .body(Boolean.class);
        return Boolean.TRUE.equals(member);
    }

    @Override
    public List<UserSummary> studentsOf(long courseId) {
        List<UserSummary> body =
                core.get()
                        .uri("/internal/membership/courses/{id}/students", courseId)
                        .retrieve()
                        .body(USERS);
        return body == null ? List.of() : body;
    }

    @Override
    public Optional<CourseSummary> findCourse(long id) {
        return optional(
                () ->
                        core.get()
                                .uri("/internal/catalog/courses/{id}", id)
                                .retrieve()
                                .body(CourseSummary.class));
    }

    @Override
    public List<CourseSummary> findCourses(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/catalog/courses/by-ids", ids, COURSES);
    }

    @Override
    public Optional<ModuleSummary> findModule(long moduleId) {
        return optional(
                () ->
                        core.get()
                                .uri("/internal/catalog/modules/{id}", moduleId)
                                .retrieve()
                                .body(ModuleSummary.class));
    }

    /** 404 means "no such thing", not a failure. */
    private static <T> Optional<T> optional(Supplier<T> call) {
        try {
            return Optional.ofNullable(call.get());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private <T> List<T> post(
            String path, Object request, ParameterizedTypeReference<List<T>> type) {
        List<T> body = core.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
