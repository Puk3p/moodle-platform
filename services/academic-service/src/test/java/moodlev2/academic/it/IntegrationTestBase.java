package moodlev2.academic.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Boots the application on a random port against a throwaway MySQL (Flyway migrates and seeds it)
 * and Kafka, with identity-service played by {@link FakeIdentity}, and drives it over real HTTP
 * through the full security chain with the tokens the gateway would forward.
 *
 * <p>The containers are started once per JVM and shared by every IT class, so Spring's cached
 * context and the database stay consistent between classes. Tests never rely on a clean database:
 * each one creates its own users, classes and courses with unique ids and names.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("academic")
                    .withUsername("academic")
                    .withPassword("academic");

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    static final FakeIdentity IDENTITY = new FakeIdentity();

    static final Path UPLOADS;

    /** Far above the seeded accounts (1-4), whose ids the seeded courses use. */
    private static final AtomicLong NEXT_ID = new AtomicLong(100_000);

    static {
        MYSQL.start();
        KAFKA.start();
        try {
            IDENTITY.start();
            UPLOADS = Files.createTempDirectory("moodle-it-uploads");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.services.identity.url", IDENTITY::url);
        registry.add(
                "spring.security.oauth2.client.registration.identity.client-secret",
                () -> FakeIdentity.CLIENT_SECRET);
        // Nothing listens there: calendars and quiz lists come back empty, as when those
        // services are down.
        registry.add("app.services.progress.url", () -> "http://127.0.0.1:9");
        registry.add("app.services.assessment.url", () -> "http://127.0.0.1:9");
        registry.add("app.uploads.dir", UPLOADS::toString);
    }

    @LocalServerPort int port;

    @Autowired JdbcTemplate jdbc;

    protected ApiClient anonymous() {
        return new ApiClient(baseUrl(), null);
    }

    /** A caller presenting this token on every request. */
    protected ApiClient withToken(String token) {
        return new ApiClient(baseUrl(), () -> token);
    }

    protected TestUser student() {
        return user("STUDENT");
    }

    protected TestUser teacher() {
        return user("TEACHER");
    }

    protected TestUser admin() {
        return user("ADMIN");
    }

    /**
     * An account in identity-service. Its client asks for a fresh user token on every request, so
     * changes to the account (its class) apply at once, as they do behind the gateway.
     */
    protected TestUser user(String role) {
        long id = NEXT_ID.incrementAndGet();
        String email =
                role.toLowerCase()
                        + "-"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + "@it.test";
        IDENTITY.addUser(id, email, role);
        return new TestUser(id, email, new ApiClient(baseUrl(), () -> IDENTITY.userToken(id)));
    }

    /** A class in identity-service; returns its id. */
    protected long createClass() {
        long id = NEXT_ID.incrementAndGet();
        IDENTITY.addClass(id, "C" + id);
        return id;
    }

    protected void placeInClass(TestUser student, long classId) {
        IDENTITY.placeInClass(student.id(), classId);
    }

    /**
     * Creates a course taught by {@code teacher}, gives it one module that is current today and
     * assigns it to {@code classId}.
     */
    protected Course createCourse(TestUser teacher, long classId) {
        String code = "IT" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        teacher.api()
                .post(
                        "/api/courses/create",
                        Map.of(
                                "code",
                                code,
                                "title",
                                "Course " + code,
                                "term",
                                "Fall 2026",
                                "description",
                                "Integration test course",
                                "teacherId",
                                teacher.id()))
                .ok();

        JsonNode edit = teacher.api().get("/api/courses/" + code + "/edit").expect(200).json();
        long courseId = edit.get("id").asLong();
        Map<String, Object> module =
                Map.of(
                        "title",
                        "Module 1",
                        "description",
                        "First module",
                        "sortOrder",
                        1,
                        "startDate",
                        LocalDate.now().toString(),
                        "endDate",
                        LocalDate.now().plusMonths(1).toString(),
                        "status",
                        "current");
        teacher.api()
                .put(
                        "/api/courses/" + code,
                        Map.of(
                                "id",
                                courseId,
                                "code",
                                code,
                                "title",
                                "Course " + code,
                                "term",
                                "Fall 2026",
                                "status",
                                "PUBLISHED",
                                "description",
                                "Integration test course",
                                "modules",
                                List.of(module),
                                "selectedGroupIds",
                                List.of(classId)))
                .ok();

        long moduleId = -1;
        for (JsonNode c :
                teacher.api().get("/api/resources/options").expect(200).json().get("courses")) {
            if (code.equals(c.get("code").asText())) {
                moduleId = c.get("modules").get(0).get("id").asLong();
            }
        }
        assertThat(moduleId).as("module of %s", code).isPositive();
        return new Course(courseId, code, moduleId);
    }

    /** Waits for something that happens after an event is consumed. */
    protected static void eventually(Runnable assertion) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
        while (true) {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                if (Instant.now().isAfter(deadline)) {
                    throw e;
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    public record TestUser(long id, String email, ApiClient api) {}

    public record Course(long id, String code, long moduleId) {}
}
