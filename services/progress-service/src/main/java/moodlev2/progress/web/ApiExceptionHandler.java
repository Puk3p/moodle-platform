package moodlev2.progress.web;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Same error shape as the other services ({@code {"error": "..."}}), so the SPA handles all alike.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException ex) {
        String reason = ex.getReason() != null ? ex.getReason() : "Request failed";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", reason));
    }

    /** A service it needs is unreachable or its circuit breaker is open: fail fast, retry later. */
    @ExceptionHandler({CallNotPermittedException.class, RestClientException.class})
    ResponseEntity<Map<String, String>> dependencyUnavailable(Exception ex) {
        log.warn("Dependency unavailable: {}", ex.toString());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "5")
                .body(
                        Map.of(
                                "error",
                                "Grades and calendar are temporarily unavailable. Please try again."));
    }
}
