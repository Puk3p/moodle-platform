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
 * identity-service's directory over HTTP: client-credentials token (see {@link CoreClientConfig}),
 * short timeouts, one retry for these read-only calls, and the {@code identity} circuit breaker.
 */
@Component
@CircuitBreaker(name = "identity")
@Retry(name = "identity")
public class IdentityClient implements CoreDirectory {

    private static final ParameterizedTypeReference<List<Person>> PEOPLE =
            new ParameterizedTypeReference<>() {};

    private final RestClient identity;

    public IdentityClient(RestClient identityRestClient) {
        this.identity = identityRestClient;
    }

    @Override
    public Optional<Person> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(
                    identity.get()
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

    private <T> List<T> postList(
            String path, Object request, ParameterizedTypeReference<List<T>> type) {
        List<T> body = identity.post().uri(path).body(request).retrieve().body(type);
        return body == null ? List.of() : body;
    }
}
