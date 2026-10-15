package moodlev2.progress.it;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Plays core for progress-service: publishes a JWKS, grants client-credentials tokens, and serves
 * the internal directory, membership and exam-lock API from in-memory data the tests control.
 * Tokens for test users are signed with the same key, as core's gateway exchange would.
 */
final class FakeCore {

    static final String CLIENT_ID = "progress-service";
    static final String CLIENT_SECRET = "it-secret";

    final MockWebServer server = new MockWebServer();
    final Map<Long, Map<String, Object>> users = new ConcurrentHashMap<>();
    final Map<Long, List<Map<String, Object>>> coursesOfStudent = new ConcurrentHashMap<>();
    final Map<Long, List<Map<String, Object>>> coursesTaught = new ConcurrentHashMap<>();
    final Map<Long, List<Long>> studentsOfCourse = new ConcurrentHashMap<>();
    final Map<Long, Map<String, Object>> courses = new ConcurrentHashMap<>();
    final Map<Long, List<Map<String, Object>>> openExams = new ConcurrentHashMap<>();
    final List<String> tokenRequests = new CopyOnWriteArrayList<>();
    final AtomicBoolean down = new AtomicBoolean();

    private final ObjectMapper json = JsonMapper.builder().build();
    private final RSAKey key;

    FakeCore() {
        try {
            key = new RSAKeyGenerator(2048).keyID("it").generate();
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

    String url() {
        String u = server.url("/").toString();
        return u.substring(0, u.length() - 1);
    }

    void addUser(long id, String email, String role, Long classId) {
        users.put(
                id,
                Map.of(
                        "id",
                        id,
                        "email",
                        email,
                        "firstName",
                        "First" + id,
                        "lastName",
                        "Last" + id,
                        "roles",
                        List.of(role),
                        "active",
                        true));
    }

    /** A course taught by {@code teacherId} with these students. */
    void addCourse(long courseId, String code, long teacherId, Long... studentIds) {
        Map<String, Object> course =
                Map.of(
                        "id", courseId,
                        "code", code,
                        "name", "Course " + code,
                        "term", "Fall 2026",
                        "teacherId", teacherId);
        courses.put(courseId, course);
        coursesTaught.computeIfAbsent(teacherId, k -> new ArrayList<>()).add(course);
        studentsOfCourse.put(courseId, List.of(studentIds));
        for (Long s : studentIds) {
            coursesOfStudent.computeIfAbsent(s, k -> new ArrayList<>()).add(course);
        }
    }

    String userToken(long userId) {
        Map<String, Object> user = users.get(userId);
        return sign(
                new JWTClaimsSet.Builder()
                        .subject(String.valueOf(userId))
                        .claim("scope", "user")
                        .claim("email", user.get("email"))
                        .claim("roles", user.get("roles"))
                        .claim("sid", "session-of-" + userId));
    }

    String serviceToken() {
        return sign(new JWTClaimsSet.Builder().subject("service:other").claim("scope", "internal"));
    }

    String sign(JWTClaimsSet.Builder claims) {
        try {
            JWTClaimsSet set =
                    claims.issuer("moodle-identity")
                            .audience("moodle-internal")
                            .issueTime(new Date())
                            .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                            .build();
            SignedJWT jwt =
                    new SignedJWT(
                            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                            set);
            jwt.sign(new RSASSASigner(key));
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
        if (path.equals("/internal/oauth2/token")) {
            tokenRequests.add(request.getHeader("Authorization"));
            String expected =
                    "Basic "
                            + java.util.Base64.getEncoder()
                                    .encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes());
            if (!expected.equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            return ok(
                    Map.of(
                            "access_token",
                            sign(
                                    new JWTClaimsSet.Builder()
                                            .subject("service:" + CLIENT_ID)
                                            .claim("scope", "internal")),
                            "token_type",
                            "Bearer",
                            "expires_in",
                            300));
        }
        if (down.get()) {
            return new MockResponse().setResponseCode(503);
        }
        if (!String.valueOf(request.getHeader("Authorization")).startsWith("Bearer ")) {
            return new MockResponse().setResponseCode(401);
        }
        String[] parts = path.split("/");
        switch (path.replaceAll("\\d+", "{id}")) {
            case "/internal/directory/users/by-email" -> {
                String email = request.getRequestUrl().queryParameter("email");
                return users.values().stream()
                        .filter(u -> u.get("email").equals(email))
                        .findFirst()
                        .map(this::ok)
                        .orElse(new MockResponse().setResponseCode(404));
            }
            case "/internal/directory/users/by-emails" -> {
                List<?> emails = json.readValue(request.getBody().readUtf8(), List.class);
                return ok(
                        users.values().stream()
                                .filter(u -> emails.contains(u.get("email")))
                                .toList());
            }
            case "/internal/directory/users/by-ids" -> {
                List<?> ids = json.readValue(request.getBody().readUtf8(), List.class);
                return ok(
                        ids.stream()
                                .map(i -> users.get(((Number) i).longValue()))
                                .filter(java.util.Objects::nonNull)
                                .toList());
            }
            case "/internal/membership/students/{id}/courses" -> {
                return ok(coursesOfStudent.getOrDefault(Long.parseLong(parts[4]), List.of()));
            }
            case "/internal/membership/users/{id}/courses" -> {
                long id = Long.parseLong(parts[4]);
                List<Map<String, Object>> all =
                        new ArrayList<>(coursesTaught.getOrDefault(id, List.of()));
                coursesOfStudent.getOrDefault(id, List.of()).stream()
                        .filter(c -> !all.contains(c))
                        .forEach(all::add);
                return ok(all);
            }
            case "/internal/catalog/courses/by-ids" -> {
                List<?> ids = json.readValue(request.getBody().readUtf8(), List.class);
                return ok(
                        ids.stream()
                                .map(i -> courses.get(((Number) i).longValue()))
                                .filter(java.util.Objects::nonNull)
                                .toList());
            }
            case "/internal/membership/teachers/{id}/courses" -> {
                return ok(coursesTaught.getOrDefault(Long.parseLong(parts[4]), List.of()));
            }
            case "/internal/membership/courses/{id}/students" -> {
                return ok(
                        studentsOfCourse.getOrDefault(Long.parseLong(parts[4]), List.of()).stream()
                                .map(users::get)
                                .toList());
            }
            case "/internal/exam-locks/users/{id}" -> {
                return ok(openExams.getOrDefault(Long.parseLong(parts[4]), List.of()));
            }
            default -> {
                return new MockResponse().setResponseCode(404);
            }
        }
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
