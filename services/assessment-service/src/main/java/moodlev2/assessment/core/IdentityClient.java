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
 * identity-service's directory over HTTP: client-credentials token (see {@link CoreClientConfig}),
 * short timeouts, one retry for these read-only calls, and the {@code identity} circuit breaker.
 */
@Component
@CircuitBreaker(name = "identity")
@Retry(name = "identity")
public class IdentityClient implements DirectoryApi {

    private static final ParameterizedTypeReference<List<UserSummary>> USERS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ClassSummary>> CLASSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient identity;

    public IdentityClient(RestClient identityRestClient) {
        this.identity = identityRestClient;
    }

    @Override
    public Optional<UserSummary> findUser(long id) {
        return optional(
                () ->
                        identity.get()
                                .uri("/internal/directory/users/{id}", id)
                                .retrieve()
                                .body(UserSummary.class));
    }

    @Override
    public Optional<UserSummary> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return optional(
                () ->
                        identity.get()
                                .uri(
                                        b ->
                                                b.path("/internal/directory/users/by-email")
                                                        .queryParam("email", email)
                                                        .build())
                                .retrieve()
                                .body(UserSummary.class));
    }

    @Override
    public List<UserSummary> findUsers(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/directory/users/by-ids", ids, USERS);
    }

    @Override
    public List<UserSummary> findUsersInClasses(Collection<Long> classIds) {
        return classIds.isEmpty()
                ? List.of()
                : post("/internal/directory/users/in-classes", classIds, USERS);
    }

    @Override
    public List<ClassSummary> findClasses(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/directory/classes/by-ids", ids, CLASSES);
    }

    /** 404 means "no such account", not a failure. */
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
        List<T> body = identity.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
