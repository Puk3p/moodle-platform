package moodlev2.academic.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Plays identity-service: publishes a JWKS, grants client-credentials tokens, and answers the
 * directory API from in-memory accounts and classes the tests control. Tokens for test users are
 * signed with the same key, as the gateway's session exchange would get them.
 */
final class FakeIdentity {

    static final String CLIENT_ID = "academic-service";
    static final String CLIENT_SECRET = "it-secret";

    final AtomicBoolean down = new AtomicBoolean();

    private final MockWebServer server = new MockWebServer();
    private final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    private final Map<Long, String> classes = new ConcurrentHashMap<>();
    private final Set<Long> twoFactor = ConcurrentHashMap.newKeySet();
    private final ObjectMapper json = JsonMapper.builder().build();
    private final RSAKey key;
    private final RSAKey strangerKey;

    FakeIdentity() {
        try {
            key = new RSAKeyGenerator(2048).keyID("it").generate();
            strangerKey = new RSAKeyGenerator(2048).keyID("it").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        server.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        try {
                            return handle(request);
                        } catch (IOException e) {
                            return new MockResponse().setResponseCode(500);
                        }
                    }
                });
    }

    void start() throws IOException {
        server.start();
    }

    void stop() throws IOException {
        server.shutdown();
    }

    String url() {
        String u = server.url("/").toString();
        return u.substring(0, u.length() - 1);
    }

    void addUser(long id, String email, String role) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        user.put("email", email);
        user.put("firstName", "First" + id);
        user.put("lastName", "Last" + id);
        user.put("roles", List.of(role));
        user.put("classId", null);
        user.put("className", null);
        user.put("active", true);
        users.put(id, user);
    }

    void removeUser(long id) {
        users.remove(id);
    }

    void addClass(long id, String name) {
        classes.put(id, name);
    }

    void placeInClass(long userId, long classId) {
        Map<String, Object> user = new LinkedHashMap<>(users.get(userId));
        user.put("classId", classId);
        user.put("className", classes.get(classId));
        users.put(userId, user);
    }

    void enableTwoFactor(long userId) {
        twoFactor.add(userId);
    }

    /** What the gateway forwards for a signed-in user: their id, email, roles and class. */
    String userToken(long userId) {
        Map<String, Object> user = users.get(userId);
        JWTClaimsSet.Builder claims =
                new JWTClaimsSet.Builder()
                        .subject(String.valueOf(userId))
                        .claim("scope", "user")
                        .claim("email", user.get("email"))
                        .claim("roles", user.get("roles"))
                        .claim("sid", "session-of-" + userId);
        if (user.get("classId") != null) {
            claims.claim("classId", user.get("classId"));
        }
        return sign(claims, key);
    }

    String serviceToken() {
        return sign(
                new JWTClaimsSet.Builder().subject("service:other").claim("scope", "internal"),
                key);
    }

    /** A well-formed service token signed by a key identity-service never published. */
    String forgedServiceToken() {
        return sign(
                new JWTClaimsSet.Builder().subject("service:other").claim("scope", "internal"),
                strangerKey);
    }

    private String sign(JWTClaimsSet.Builder claims, RSAKey signingKey) {
        try {
            JWTClaimsSet set =
                    claims.issuer("moodle-identity")
                            .audience("moodle-internal")
                            .issueTime(new Date())
                            .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                            .build();
            SignedJWT jwt =
                    new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256)
                                    .keyID(signingKey.getKeyID())
                                    .build(),
                            set);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private MockResponse handle(RecordedRequest request) throws IOException {
        String path = request.getRequestUrl().encodedPath();
        if (path.equals("/internal/.well-known/jwks.json")) {
            return ok(new JWKSet(key.toPublicJWK()).toJSONObject());
        }
        if (down.get()) {
            return new MockResponse().setResponseCode(503);
        }
        if (path.equals("/internal/oauth2/token")) {
            String expected =
                    "Basic "
                            + Base64.getEncoder()
                                    .encodeToString(
                                            (CLIENT_ID + ":" + CLIENT_SECRET)
                                                    .getBytes(StandardCharsets.UTF_8));
            if (!expected.equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            return ok(
                    Map.of(
                            "access_token",
                            sign(
                                    new JWTClaimsSet.Builder()
                                            .subject("service:" + CLIENT_ID)
                                            .claim("scope", "internal"),
                                    key),
                            "token_type",
                            "Bearer",
                            "expires_in",
                            300));
        }
        if (!String.valueOf(request.getHeader("Authorization")).startsWith("Bearer ")) {
            return new MockResponse().setResponseCode(401);
        }
        String[] parts = path.split("/");
        switch (path.replaceAll("/\\d+", "/{id}")) {
            case "/internal/directory/users/{id}" -> {
                return found(users.get(Long.parseLong(parts[4])));
            }
            case "/internal/directory/users/{id}/account" -> {
                long id = Long.parseLong(parts[4]);
                Map<String, Object> user = users.get(id);
                return user == null
                        ? notFound()
                        : ok(Map.of("user", user, "twoFaEnabled", twoFactor.contains(id)));
            }
            case "/internal/directory/users/by-email" -> {
                String email = request.getRequestUrl().queryParameter("email");
                return found(
                        users.values().stream()
                                .filter(u -> u.get("email").equals(email))
                                .findFirst()
                                .orElse(null));
            }
            case "/internal/directory/users/by-ids" -> {
                return ok(
                        longs(request).stream().map(users::get).filter(Objects::nonNull).toList());
            }
            case "/internal/directory/users/by-emails" -> {
                List<?> emails = json.readValue(request.getBody().readUtf8(), List.class);
                return ok(
                        users.values().stream()
                                .filter(u -> emails.contains(u.get("email")))
                                .toList());
            }
            case "/internal/directory/users/in-classes" -> {
                Collection<Long> classIds = longs(request);
                return ok(
                        users.values().stream()
                                .filter(
                                        u ->
                                                u.get("classId") != null
                                                        && classIds.contains(
                                                                ((Number) u.get("classId"))
                                                                        .longValue()))
                                .toList());
            }
            case "/internal/directory/classes/{id}" -> {
                long id = Long.parseLong(parts[4]);
                return classes.containsKey(id)
                        ? ok(Map.of("id", id, "name", classes.get(id)))
                        : notFound();
            }
            case "/internal/directory/classes/by-ids" -> {
                return ok(
                        longs(request).stream()
                                .filter(classes::containsKey)
                                .map(id -> Map.of("id", id, "name", classes.get(id)))
                                .toList());
            }
            default -> {
                return notFound();
            }
        }
    }

    private List<Long> longs(RecordedRequest request) throws IOException {
        List<?> ids = json.readValue(request.getBody().readUtf8(), List.class);
        return ids.stream().map(i -> ((Number) i).longValue()).toList();
    }

    private MockResponse found(Object body) {
        return body == null ? notFound() : ok(body);
    }

    private static MockResponse notFound() {
        return new MockResponse().setResponseCode(404);
    }

    private MockResponse ok(Object body) {
        try {
            return new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(json.writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
