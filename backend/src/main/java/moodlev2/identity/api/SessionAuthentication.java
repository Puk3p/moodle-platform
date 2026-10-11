package moodlev2.identity.api;

import java.util.Optional;

/**
 * Authenticates a request that only exposes raw headers (the WebSocket upgrade), with the same
 * checks as every HTTP request: expiry, idle time, same browser, active account.
 */
public interface SessionAuthentication {

    Optional<AuthenticatedSession> fromCookieHeader(
            String cookieHeader, String userAgent, String remoteAddress);

    /**
     * @param tokenHash identifies the session, so sockets can be closed when it is revoked (see
     *     {@link SessionsRevokedEvent})
     */
    record AuthenticatedSession(long userId, String email, String tokenHash) {}
}
