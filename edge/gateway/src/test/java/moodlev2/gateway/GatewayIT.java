package moodlev2.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

/**
 * The gateway between a browser and three stand-ins: identity-service (token endpoint, session
 * resolution and its own cookie-authenticated routes), and core and messaging-service, which only
 * ever see user tokens (they record what reaches them).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIT {

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");
    static final MockWebServer IDENTITY = new MockWebServer();
    static final MockWebServer MESSAGING = new MockWebServer();
    static final MockWebServer CORE = new MockWebServer();

    /** Session token → user token, as identity-service resolves them (for the matching browser). */
    static final Map<String, String> SESSIONS = new ConcurrentHashMap<>();

    static final AtomicInteger RESOLVE_CALLS = new AtomicInteger();
    static final BlockingQueue<RecordedRequest> IDENTITY_REQUESTS = new LinkedBlockingQueue<>();
    static final BlockingQueue<RecordedRequest> MESSAGING_REQUESTS = new LinkedBlockingQueue<>();
    static final BlockingQueue<RecordedRequest> CORE_REQUESTS = new LinkedBlockingQueue<>();
    static final String BROWSER = "Mozilla/5.0 (it)";

    static {
        KAFKA.start();
        IDENTITY.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        return identity(request);
                    }
                });
        MESSAGING.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        MESSAGING_REQUESTS.add(request);
                        if (request.getPath().startsWith("/ws/")) {
                            return new MockResponse()
                                    .withWebSocketUpgrade(new WebSocketListener() {});
                        }
                        return new MockResponse()
                                .setResponseCode(request.getMethod().equals("POST") ? 201 : 200)
                                .setHeader("Content-Type", "application/json")
                                .setBody("{\"from\":\"messaging\"}");
                    }
                });
        CORE.setDispatcher(
                new Dispatcher() {
                    @Override
                    public MockResponse dispatch(RecordedRequest request) {
                        CORE_REQUESTS.add(request);
                        return new MockResponse().setBody("from core " + request.getPath());
                    }
                });
        try {
            IDENTITY.start();
            MESSAGING.start();
            CORE.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("IDENTITY_URL", () -> base(IDENTITY));
        registry.add("MESSAGING_URL", () -> base(MESSAGING));
        registry.add("CORE_URL", () -> base(CORE));
        registry.add(
                "spring.security.oauth2.client.registration.identity.client-secret",
                () -> "gw-secret");
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.session.cookie-name", () -> "mdl_session");
    }

    @AfterAll
    static void stop() {
        for (MockWebServer server : List.of(IDENTITY, MESSAGING, CORE)) {
            try {
                server.shutdown();
            } catch (IOException e) {
                // A test WebSocket may still be draining; the JVM is about to exit anyway.
            }
        }
    }

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach
    void reset() {
        IDENTITY_REQUESTS.clear();
        MESSAGING_REQUESTS.clear();
        CORE_REQUESTS.clear();
    }

    @Test
    void identityRoutesPassThroughWithTheBrowsersCookies() throws Exception {
        HttpResponse<String> response =
                send(get("/api/auth/session").header("Cookie", "mdl_session=abc").build());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("from identity");

        RecordedRequest atIdentity = take(IDENTITY_REQUESTS, "/api/auth/session");
        assertThat(atIdentity.getHeader("Cookie")).contains("mdl_session=abc");
        assertThat(atIdentity.getHeader("Authorization")).isNull();
        assertThat(atIdentity.getHeader("X-Forwarded-For")).isNotBlank();
    }

    @Test
    void theClientAddressReachesIdentityThroughATrustedProxy() throws Exception {
        // The test client connects from loopback, a trusted proxy (as nginx would be): the chain
        // it reports is kept and its own address appended.
        send(get("/api/auth/session").header("X-Forwarded-For", "203.0.113.9").build());
        RecordedRequest atIdentity = take(IDENTITY_REQUESTS, "/api/auth/session");
        assertThat(atIdentity.getHeader("X-Forwarded-For")).isEqualTo("203.0.113.9, 127.0.0.1");
    }

    @Test
    void coreGetsAUserTokenAndNeverTheCookie() throws Exception {
        assertThat(send(get("/api/courses").build()).statusCode()).isEqualTo(401);
        assertThat(CORE_REQUESTS).isEmpty();

        String session = newSession("user-token-core");
        HttpResponse<String> response =
                send(get("/api/courses").header("Cookie", "mdl_session=" + session).build());
        assertThat(response.statusCode()).isEqualTo(200);
        RecordedRequest atCore = take(CORE_REQUESTS, "/api/courses");
        assertThat(atCore.getHeader("Authorization")).isEqualTo("Bearer user-token-core");
        assertThat(atCore.getHeader("Cookie")).isNull();
    }

    @Test
    void theProfileIsCoresAndTheRestOfTheAccountIsIdentitys() throws Exception {
        String session = newSession("user-token-me");
        String cookie = "mdl_session=" + session;

        send(get("/api/users/me").header("Cookie", cookie).build());
        assertThat(take(CORE_REQUESTS, "/api/users/me").getHeader("Authorization"))
                .isEqualTo("Bearer user-token-me");

        send(get("/api/users/me/picture").header("Cookie", cookie).build());
        assertThat(take(IDENTITY_REQUESTS, "/api/users/me/picture").getHeader("Cookie"))
                .contains(cookie);
        send(get("/api/users/sessions").header("Cookie", cookie).build());
        take(IDENTITY_REQUESTS, "/api/users/sessions");
    }

    @Test
    void corsPreflightIsAnsweredHereForEveryService() throws Exception {
        for (String path : List.of("/api/courses", "/api/auth/login", "/api/chat/messages")) {
            HttpResponse<String> preflight =
                    send(
                            get(path)
                                    .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                                    .header("Origin", "http://localhost:4200")
                                    .header("Access-Control-Request-Method", "POST")
                                    .header("Access-Control-Request-Headers", "x-xsrf-token")
                                    .build());
            assertThat(preflight.statusCode()).as(path).isEqualTo(200);
            assertThat(preflight.headers().firstValue("Access-Control-Allow-Origin"))
                    .as(path)
                    .contains("http://localhost:4200");
            assertThat(preflight.headers().firstValue("Access-Control-Allow-Credentials"))
                    .as(path)
                    .contains("true");
        }
        HttpResponse<String> foreign =
                send(
                        get("/api/courses")
                                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                                .header("Origin", "https://evil.example")
                                .header("Access-Control-Request-Method", "POST")
                                .build());
        assertThat(foreign.statusCode()).isEqualTo(403);
        assertThat(IDENTITY_REQUESTS.stream().anyMatch(r -> "OPTIONS".equals(r.getMethod())))
                .isFalse();
        assertThat(CORE_REQUESTS).isEmpty();
    }

    @Test
    void internalApisAreNeverReachable() throws Exception {
        assertThat(send(get("/internal/directory/users/1").build()).statusCode()).isEqualTo(404);
        assertThat(send(get("/internal/.well-known/jwks.json").build()).statusCode())
                .isEqualTo(404);
        assertThat(
                        IDENTITY_REQUESTS.stream()
                                .anyMatch(r -> r.getPath().startsWith("/internal/directory")))
                .isFalse();
    }

    @Test
    void chatNeedsASessionIdentityAccepts() throws Exception {
        assertThat(send(get("/api/chat/status").build()).statusCode()).isEqualTo(401);
        assertThat(
                        send(get("/api/chat/status")
                                        .header("Cookie", "mdl_session=unknown")
                                        .build())
                                .statusCode())
                .isEqualTo(401);
        assertThat(MESSAGING_REQUESTS).isEmpty();
    }

    @Test
    void theSessionIsExchangedForAUserTokenAndTheCookieStaysBehind() throws Exception {
        String session = newSession("user-token-1");
        int before = RESOLVE_CALLS.get();

        for (int i = 0; i < 3; i++) {
            HttpResponse<String> response =
                    send(
                            get("/api/chat/status")
                                    .header("Cookie", "mdl_session=" + session)
                                    // Whatever a client sends here is replaced.
                                    .header("Authorization", "Bearer forged")
                                    .build());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).contains("from");
            RecordedRequest atMessaging = MESSAGING_REQUESTS.poll(5, TimeUnit.SECONDS);
            assertThat(atMessaging.getHeader("Authorization")).isEqualTo("Bearer user-token-1");
            assertThat(atMessaging.getHeader("Cookie")).isNull();
        }
        // Cached after the first exchange.
        assertThat(RESOLVE_CALLS.get() - before).isEqualTo(1);
    }

    @Test
    void anotherBrowserWithTheSameCookieIsNeverServedFromCache() throws Exception {
        String session = newSession("user-token-2");
        assertThat(
                        send(get("/api/chat/status")
                                        .header("Cookie", "mdl_session=" + session)
                                        .build())
                                .statusCode())
                .isEqualTo(200);

        HttpResponse<String> thief =
                send(
                        get("/api/chat/status")
                                .setHeader("User-Agent", "curl/8")
                                .header("Cookie", "mdl_session=" + session)
                                .build());
        assertThat(thief.statusCode()).isEqualTo(401);
    }

    @Test
    void stateChangingChatCallsNeedTheCsrfHeader() throws Exception {
        String session = newSession("user-token-3");
        String cookies = "mdl_session=" + session + "; XSRF-TOKEN=csrf-1";

        assertThat(send(post("/api/chat/messages").header("Cookie", cookies).build()).statusCode())
                .isEqualTo(403);
        assertThat(
                        send(post("/api/chat/messages")
                                        .header("Cookie", cookies)
                                        .header("X-XSRF-TOKEN", "other")
                                        .build())
                                .statusCode())
                .isEqualTo(403);
        assertThat(MESSAGING_REQUESTS).isEmpty();

        HttpResponse<String> ok =
                send(
                        post("/api/chat/messages")
                                .header("Cookie", cookies)
                                .header("X-XSRF-TOKEN", "csrf-1")
                                .build());
        assertThat(ok.statusCode()).isEqualTo(201);
    }

    @Test
    void aRevokedSessionIsDroppedFromTheCacheAtOnce() throws Exception {
        String session = newSession("user-token-4");
        assertThat(
                        send(get("/api/chat/status")
                                        .header("Cookie", "mdl_session=" + session)
                                        .build())
                                .statusCode())
                .isEqualTo(200);

        SESSIONS.remove(session); // identity-service no longer accepts it
        publishRevoked(sha256(session));

        Instant deadline = Instant.now().plusSeconds(15);
        int status = 200;
        while (status == 200 && Instant.now().isBefore(deadline)) {
            Thread.sleep(200);
            status =
                    send(get("/api/chat/status").header("Cookie", "mdl_session=" + session).build())
                            .statusCode();
        }
        assertThat(status).isEqualTo(401);
    }

    @Test
    void theWebSocketUpgradeCarriesTheUserToken() throws Exception {
        String session = newSession("user-token-5");
        WebSocket socket =
                http.newWebSocketBuilder()
                        .header("Cookie", "mdl_session=" + session)
                        .header("User-Agent", BROWSER)
                        .buildAsync(
                                URI.create("ws://127.0.0.1:" + port + "/ws/websocket"),
                                new WebSocket.Listener() {})
                        .get(10, TimeUnit.SECONDS);
        RecordedRequest upgrade = take(MESSAGING_REQUESTS, "/ws/websocket");
        assertThat(upgrade.getHeader("Authorization")).isEqualTo("Bearer user-token-5");
        assertThat(upgrade.getHeader("Cookie")).isNull();
        socket.abort();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static MockResponse identity(RecordedRequest request) {
        IDENTITY_REQUESTS.add(request);
        String path = request.getPath();
        if (path.equals("/internal/oauth2/token")) {
            return json(
                    Map.of(
                            "access_token",
                            "gateway-service-token",
                            "token_type",
                            "Bearer",
                            "expires_in",
                            300));
        }
        if (path.equals("/internal/sessions/resolve")) {
            RESOLVE_CALLS.incrementAndGet();
            if (!"Bearer gateway-service-token".equals(request.getHeader("Authorization"))) {
                return new MockResponse().setResponseCode(401);
            }
            try {
                Map<?, ?> body =
                        new ObjectMapper().readValue(request.getBody().readUtf8(), Map.class);
                String token = (String) body.get("token");
                String userToken = SESSIONS.get(token);
                if (userToken == null || !BROWSER.equals(body.get("userAgent"))) {
                    return new MockResponse().setResponseCode(401);
                }
                return json(
                        Map.of(
                                "userId",
                                7,
                                "email",
                                "student@it.test",
                                "roles",
                                List.of("STUDENT"),
                                "sessionHash",
                                sha256(token),
                                "accessToken",
                                userToken,
                                "expiresIn",
                                60));
            } catch (IOException e) {
                return new MockResponse().setResponseCode(400);
            }
        }
        return new MockResponse().setBody("from identity " + path);
    }

    private static String newSession(String userToken) {
        String session = "session-" + System.nanoTime();
        SESSIONS.put(session, userToken);
        return session;
    }

    private void publishRevoked(String tokenHash) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            var record =
                    new ProducerRecord<>(
                            "identity.session",
                            "7",
                            "{\"userId\":7,\"tokenHashes\":[\"" + tokenHash + "\"]}");
            record.headers()
                    .add("event-type", "identity.SessionsRevoked".getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }
    }

    private static MockResponse json(Object body) {
        try {
            return new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(new ObjectMapper().writeValueAsString(body));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static RecordedRequest take(BlockingQueue<RecordedRequest> queue, String path)
            throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(5);
        while (Instant.now().isBefore(deadline)) {
            RecordedRequest r = queue.poll(200, TimeUnit.MILLISECONDS);
            if (r != null && r.getPath().equals(path)) {
                return r;
            }
        }
        throw new AssertionError("no request to " + path);
    }

    private static String base(MockWebServer server) {
        String u = server.url("/").toString();
        return u.substring(0, u.length() - 1);
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", BROWSER);
    }

    private HttpRequest.Builder post(String path) {
        return get(path)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json");
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
