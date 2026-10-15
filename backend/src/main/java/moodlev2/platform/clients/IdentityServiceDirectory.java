package moodlev2.platform.clients;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import moodlev2.academic.directory.AccountSummary;
import moodlev2.academic.directory.ClassSummary;
import moodlev2.academic.directory.DirectoryApi;
import moodlev2.academic.directory.UserSummary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * {@link DirectoryApi} served by identity-service's internal API: one retry for these read-only
 * calls and the {@code identity} circuit breaker. Nothing here degrades quietly; without the
 * directory a page cannot tell who may see what, so the request fails with 503 instead.
 */
@Component
@CircuitBreaker(name = "identity")
@Retry(name = "identity")
public class IdentityServiceDirectory implements DirectoryApi {

    private static final ParameterizedTypeReference<List<UserSummary>> USERS =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ClassSummary>> CLASSES =
            new ParameterizedTypeReference<>() {};

    private final RestClient identity;

    public IdentityServiceDirectory(RestClient identityRestClient) {
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
    public Optional<AccountSummary> findAccount(long id) {
        return optional(
                () ->
                        identity.get()
                                .uri("/internal/directory/users/{id}/account", id)
                                .retrieve()
                                .body(AccountSummary.class));
    }

    @Override
    public List<UserSummary> findUsers(Collection<Long> ids) {
        return ids.isEmpty() ? List.of() : post("/internal/directory/users/by-ids", ids, USERS);
    }

    @Override
    public List<UserSummary> findUsersByEmail(Collection<String> emails) {
        return emails.isEmpty()
                ? List.of()
                : post("/internal/directory/users/by-emails", emails, USERS);
    }

    @Override
    public List<UserSummary> findUsersInClasses(Collection<Long> classIds) {
        return classIds.isEmpty()
                ? List.of()
                : post("/internal/directory/users/in-classes", classIds, USERS);
    }

    @Override
    public Optional<ClassSummary> findClass(long id) {
        return optional(
                () ->
                        identity.get()
                                .uri("/internal/directory/classes/{id}", id)
                                .retrieve()
                                .body(ClassSummary.class));
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
