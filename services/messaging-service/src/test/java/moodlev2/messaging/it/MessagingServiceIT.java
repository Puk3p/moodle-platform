package moodlev2.messaging.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * messaging-service on its own: its database and Kafka in containers, core played by {@link
 * FakeCore}. Requests carry the user tokens the gateway would forward.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MessagingServiceIT {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("messaging");
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");
    static final FakeCore CORE = new FakeCore();

    static {
        MYSQL.start();
        KAFKA.start();
        try {
            CORE.start();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.core.url", CORE::url);
        registry.add("app.assessment.url", CORE::url);
        registry.add(
                "spring.security.oauth2.client.registration.core.client-secret",
                () -> FakeCore.CLIENT_SECRET);
    }

    @AfterAll
    static void stopCore() throws java.io.IOException {
        CORE.server.shutdown();
    }

    private static final long TEACHER = 1, STUDENT = 2, OTHER_STUDENT = 3;

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void people() {
        CORE.down.set(false);
        CORE.users.clear();
        CORE.coursesOfStudent.clear();
        CORE.coursesTaught.clear();
        CORE.studentsOfCourse.clear();
        CORE.openExams.clear();
        CORE.addUser(TEACHER, "teacher@it.test", "TEACHER", null);
        CORE.addUser(STUDENT, "student@it.test", "STUDENT", 7L);
        CORE.addUser(OTHER_STUDENT, "other@it.test", "STUDENT", null);
        CORE.addCourse(10, "CS101", TEACHER, STUDENT);
    }

    @Test
    void onlyUserTokensFromCoreAreAccepted() throws Exception {
        assertThat(get("/api/chat/status", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/chat/status", "not-a-jwt").statusCode()).isEqualTo(401);
        // Signed by core, but a service token: not a user.
        assertThat(get("/api/chat/status", CORE.serviceToken()).statusCode()).isEqualTo(403);

        HttpResponse<String> status = get("/api/chat/status", CORE.userToken(STUDENT));
        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(json.readTree(status.body()).get("available").asBoolean()).isTrue();
        // It looked the student up with its own client-credentials token.
        assertThat(CORE.tokenRequests).isNotEmpty();
    }

    @Test
    void teacherAndStudentChatAndContactsComeFromCoreMembership() throws Exception {
        JsonNode contacts =
                json.readTree(get("/api/chat/contacts", CORE.userToken(STUDENT)).body());
        assertThat(contacts.toString()).contains("teacher@it.test").contains("CS101");

        HttpResponse<String> sent =
                post(
                        "/api/chat/messages",
                        Map.of(
                                "recipient",
                                "student@it.test",
                                "content",
                                "Welcome " + System.nanoTime()),
                        CORE.userToken(TEACHER));
        assertThat(sent.statusCode()).isEqualTo(201);
        String content = json.readTree(sent.body()).get("content").asText();

        assertThat(get("/api/chat/history", CORE.userToken(STUDENT)).body()).contains(content);

        // Student to student is refused, as in core.
        assertThat(
                        post(
                                        "/api/chat/messages",
                                        Map.of("recipient", "other@it.test", "content", "hi"),
                                        CORE.userToken(STUDENT))
                                .statusCode())
                .isEqualTo(403);
    }

    @Test
    void anOpenMessagingBlockingQuizLocksTheStudent() throws Exception {
        CORE.openExams.put(
                STUDENT,
                List.of(Map.of("startedAt", Instant.now().toString(), "durationMinutes", 30)));

        JsonNode status = json.readTree(get("/api/chat/status", CORE.userToken(STUDENT)).body());
        assertThat(status.get("reason").asText()).isEqualTo("QUIZ_IN_PROGRESS");
        assertThat(
                        post(
                                        "/api/chat/messages",
                                        Map.of("recipient", "teacher@it.test", "content", "help"),
                                        CORE.userToken(STUDENT))
                                .statusCode())
                .isEqualTo(423);
    }

    @Test
    void renamesAndDeletionsInCoreReachTheHistoryThroughKafka() throws Exception {
        String content = "Before rename " + System.nanoTime();
        assertThat(
                        post(
                                        "/api/chat/messages",
                                        Map.of("recipient", "student@it.test", "content", content),
                                        CORE.userToken(TEACHER))
                                .statusCode())
                .isEqualTo(201);

        CORE.addUser(STUDENT, "renamed@it.test", "STUDENT", 7L);
        publish(
                "identity.user",
                String.valueOf(STUDENT),
                "identity.UserEmailChanged",
                Map.of(
                        "userId",
                        STUDENT,
                        "oldEmail",
                        "student@it.test",
                        "newEmail",
                        "renamed@it.test"));
        await(
                () ->
                        historyOf(STUDENT).contains(content)
                                && historyOf(STUDENT).contains("renamed@it.test"));

        publish(
                "identity.user",
                String.valueOf(STUDENT),
                "identity.UserDeleted",
                Map.of("userId", STUDENT, "email", "renamed@it.test"));
        await(() -> !historyOf(TEACHER).contains(content));
    }

    @Test
    void whenCoreIsDownCallsFailFastWith503() throws Exception {
        CORE.down.set(true);
        HttpResponse<String> response = get("/api/chat/status", CORE.userToken(STUDENT));
        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.headers().firstValue("Retry-After")).isPresent();
    }

    private String historyOf(long userId) {
        try {
            return get("/api/chat/history", CORE.userToken(userId)).body();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void publish(String topic, String key, String type, Object payload) throws Exception {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                    new ProducerRecord<>(topic, key, json.writeValueAsString(payload));
            record.headers().add("event-type", type.getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }
    }

    private static void await(Supplier<Boolean> condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("condition not met in time");
    }

    private HttpResponse<String> get(String path, String bearer) throws Exception {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Object body, String bearer) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Authorization", "Bearer " + bearer)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
