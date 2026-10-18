package moodlev2.progress.clients;

import java.util.function.Predicate;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Which failed calls to another service get their one retry: anything but a client error, and a
 * 401. A 401 means this service's token was refused; the interceptor has dropped it, so the retry
 * carries a fresh one. Other client errors (404, 400, 403) would only fail again.
 */
public class RetryableCallFailure implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable failure) {
        return !(failure instanceof HttpClientErrorException e)
                || e.getStatusCode() == HttpStatus.UNAUTHORIZED;
    }
}
