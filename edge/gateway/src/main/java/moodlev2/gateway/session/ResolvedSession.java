package moodlev2.gateway.session;

import java.util.List;

/**
 * What identity-service says about a browser session (its {@code /internal/sessions/resolve}
 * answer).
 *
 * @param sessionHash SHA-256 of the session token, as identity-service's revocation events name it
 * @param accessToken the user token forwarded to services
 */
public record ResolvedSession(
        long userId,
        String email,
        List<String> roles,
        Long classId,
        String sessionHash,
        String accessToken,
        long expiresIn) {

    public ResolvedSession {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
