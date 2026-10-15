package moodlev2.progress.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
 * progress-service on its own: its database and Kafka in containers, identity- and academic-service
 * played by FakeServices.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProgressServiceIT {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("progress");
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");
    static final FakeServices SERVICES = new FakeServices();

    static {
        MYSQL.start();
        KAFKA.start();
        try {
            SERVICES.start();
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
        // One fake plays both identity-service and academic-service.
        registry.add("app.identity.url", SERVICES::url);
        registry.add("app.academic.url", SERVICES::url);
        registry.add(
                "spring.security.oauth2.client.registration.identity.client-secret",
                () -> FakeServices.CLIENT_SECRET);
    }

    @AfterAll
    static void stopCore() {
        try {
            SERVICES.server.shutdown();
        } catch (java.io.IOException e) {
            // Going away anyway.
        }
    }

    private static final long TEACHER = 1, STUDENT = 2, ADMIN = 3;

    @LocalServerPort int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void people() {
        SERVICES.users.clear();
        SERVICES.coursesOfStudent.clear();
        SERVICES.coursesTaught.clear();
        SERVICES.studentsOfCourse.clear();
        SERVICES.courses.clear();
        SERVICES.addUser(TEACHER, "teacher@it.test", "TEACHER", null);
        SERVICES.addUser(STUDENT, "student@it.test", "STUDENT", null);
        SERVICES.addUser(ADMIN, "admin@it.test", "ADMIN", null);
        SERVICES.addCourse(10, "CS101", TEACHER, STUDENT);
    }

    @Test
    void onlyUserTokensReachTheApiAndOnlyAdminsTheGradebook() throws Exception {
        assertThat(get("/api/grades", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/grades", SERVICES.serviceToken()).statusCode()).isEqualTo(403);
        assertThat(get("/api/grades", SERVICES.userToken(STUDENT)).statusCode()).isEqualTo(200);
        assertThat(get("/api/admin/grades", SERVICES.userToken(STUDENT)).statusCode())
                .isEqualTo(403);
        assertThat(get("/api/admin/grades", SERVICES.userToken(ADMIN)).statusCode()).isEqualTo(200);
    }

    @Test
    void anAssignmentDeadlineArrivesOnceEvenIfTheEventIsDeliveredTwice() throws Exception {
        long assignmentId = System.nanoTime() % 1_000_000;
        String title = "Essay " + assignmentId;
        Map<String, Object> event =
                Map.of(
                        "courseId",
                        10,
                        "assignmentId",
                        assignmentId,
                        "title",
                        title,
                        "dueDate",
                        LocalDate.now().plusDays(7) + "T23:59:00");
        publish("academic.coursework", "10", "academic.AssignmentPublished", event);
        publish("academic.coursework", "10", "academic.AssignmentPublished", event);

        eventually(() -> calendarOf(STUDENT).contains(title + " (Deadline)"));
        JsonNode calendar = json.readTree(calendarOf(STUDENT));
        long copies = 0;
        for (JsonNode e : calendar.get("events")) {
            if (e.get("title").asText().equals(title + " (Deadline)")) {
                copies++;
                assertThat(e.get("courseCode").asText()).isEqualTo("CS101");
            }
        }
        assertThat(copies).isEqualTo(1);

        // academic-service asks for course calendars through the internal API.
        HttpResponse<String> internal =
                post("/internal/calendar/courses", List.of(10), SERVICES.serviceToken());
        assertThat(internal.statusCode()).isEqualTo(200);
        assertThat(internal.body()).contains(title);
        assertThat(
                        post("/internal/calendar/courses", List.of(10), SERVICES.userToken(STUDENT))
                                .statusCode())
                .isEqualTo(403);
    }

    @Test
    void gradesShowPerCourseAndGoWithADeletedAccount() throws Exception {
        sql(
                "INSERT INTO grades (user_id, course_id, item_name, score_received, max_score, graded_at, type_icon)"
                        + " VALUES (?, 10, 'Quiz 1', 18.5, 20, CURRENT_DATE, 'quiz')",
                STUDENT);

        JsonNode page = json.readTree(get("/api/grades", SERVICES.userToken(STUDENT)).body());
        assertThat(page.get("courses").toString()).contains("CS101").contains("18.5/20");

        JsonNode gradebook =
                json.readTree(get("/api/admin/grades", SERVICES.userToken(ADMIN)).body());
        assertThat(gradebook.toString()).contains("First2 Last2").contains("CS101");

        publish(
                "identity.user",
                String.valueOf(STUDENT),
                "identity.UserDeleted",
                Map.of("userId", STUDENT, "email", "student@it.test"));
        eventually(() -> count("SELECT COUNT(*) FROM grades WHERE user_id = " + STUDENT) == 0);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String calendarOf(long userId) {
        try {
            return get("/api/calendar", SERVICES.userToken(userId)).body();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void sql(String statement, Object... args) throws Exception {
        try (Connection c =
                        DriverManager.getConnection(
                                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                var ps = c.prepareStatement(statement)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    private static long count(String query) {
        try (Connection c =
                        DriverManager.getConnection(
                                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
                ResultSet rs = c.createStatement().executeQuery(query)) {
            rs.next();
            return rs.getLong(1);
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
            var record = new ProducerRecord<>(topic, key, json.writeValueAsString(payload));
            record.headers().add("event-type", type.getBytes(StandardCharsets.UTF_8));
            producer.send(record).get();
        }
    }

    private static void eventually(Supplier<Boolean> condition) throws InterruptedException {
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
