package moodlev2.identity.web.internal;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.DirectoryApi;
import moodlev2.identity.api.UserSummary;
import moodlev2.identity.application.auth.SessionService;
import moodlev2.identity.infrastructure.security.InternalClients;
import moodlev2.identity.infrastructure.security.InternalJwtKeys;
import moodlev2.identity.infrastructure.security.InternalTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token plumbing for the gateway and the services. Never routed by the gateway; reachable only on
 * the internal network.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final InternalClients clients;
    private final InternalTokens tokens;
    private final InternalJwtKeys keys;
    private final SessionService sessionService;
    private final DirectoryApi directory;

    /** The public signing keys, for services validating tokens. */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return keys.publicJwks().toJSONObject();
    }

    /** OAuth2 client-credentials grant (RFC 6749 §4.4) with HTTP Basic client authentication. */
    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> token(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                    String authorization,
            @RequestParam("grant_type") String grantType) {
        if (!"client_credentials".equals(grantType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
        }
        String[] credentials = basicCredentials(authorization);
        if (credentials == null || !clients.authenticates(credentials[0], credentials[1])) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"internal\"")
                    .body(Map.of("error", "invalid_client"));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(
                        Map.of(
                                "access_token",
                                tokens.forService(credentials[0]),
                                "token_type",
                                "Bearer",
                                "expires_in",
                                InternalTokens.SERVICE_TOKEN_LIFETIME.toSeconds(),
                                "scope",
                                "internal"));
    }

    /**
     * The gateway's cookie-to-token exchange: checks the browser's session exactly like an HTTP
     * request to this application (expiry, idle time, same browser, active account) and returns a
     * short-lived user token for the downstream services.
     */
    @PostMapping("/sessions/resolve")
    public ResponseEntity<ResolvedSession> resolve(@RequestBody ResolveRequest request) {
        if (request == null || request.token() == null || request.token().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return sessionService
                .resolve(request.token(), request.userAgent(), request.remoteAddress())
                .flatMap(
                        resolved ->
                                directory
                                        .findUser(resolved.user().getId())
                                        .map(user -> toResponse(user, resolved.tokenHash())))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private ResolvedSession toResponse(UserSummary user, String sessionHash) {
        return new ResolvedSession(
                user.id(),
                user.email(),
                user.roles().stream().map(Enum::name).sorted().toList(),
                user.classId(),
                sessionHash,
                tokens.forUser(user, sessionHash),
                InternalTokens.USER_TOKEN_LIFETIME.toSeconds());
    }

    private static String[] basicCredentials(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String decoded =
                    new String(
                            Base64.getDecoder().decode(authorization.substring(6).trim()),
                            StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            return colon < 0
                    ? null
                    : new String[] {decoded.substring(0, colon), decoded.substring(colon + 1)};
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public record ResolveRequest(String token, String userAgent, String remoteAddress) {}

    /**
     * @param sessionHash identifies the browser session (SHA-256 of its token)
     * @param accessToken RS256 user token for the services; valid {@code expiresIn} seconds
     */
    public record ResolvedSession(
            long userId,
            String email,
            List<String> roles,
            Long classId,
            String sessionHash,
            String accessToken,
            long expiresIn) {}
}
