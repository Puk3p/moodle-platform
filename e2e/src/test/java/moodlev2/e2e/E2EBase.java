package moodlev2.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Base for end-to-end tests: everything goes through the gateway of a running stack, as the SPA
 * would. The database is touched only to give test accounts a role (there is no API for that).
 *
 * <p>Tests never rely on a clean database: each creates its own users, classes and courses.
 */
public abstract class E2EBase {

    static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    static final String DB_URL =
            System.getProperty("e2e.dbUrl", "jdbc:mysql://localhost:3307/moodle");
    static final String DB_USER = System.getProperty("e2e.dbUser", "core");
    static final String DB_PASSWORD = System.getProperty("e2e.dbPassword", "core-dev");

    static void sql(String statement, Object... args) {
        try (Connection c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                PreparedStatement ps = c.prepareStatement(statement)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Services react to events asynchronously: poll until it holds. */
    static void eventually(Supplier<Boolean> condition) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("condition not met within 30s");
    }

    protected ApiClient anonymous() {
        return new ApiClient(BASE_URL);
    }

    protected TestUser student() {
        return register("STUDENT");
    }

    protected TestUser teacher() {
        return register("TEACHER");
    }

    protected TestUser admin() {
        return register("ADMIN");
    }

    /**
     * Registers through the public API (every account starts as a student), then sets the role
     * directly. Roles are re-read from the database on every request, so it applies at once.
     */
    protected TestUser register(String role) {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = role.toLowerCase() + "-" + tag + "@it.test";
        String password = "Pa55word-" + tag;
        ApiClient client = anonymous();
        JsonNode body =
                client.post(
                                "/api/auth/register",
                                Map.of(
                                        "email",
                                        email,
                                        "password",
                                        password,
                                        "firstName",
                                        "First" + tag,
                                        "lastName",
                                        "Last" + tag))
                        .expect(200)
                        .json();
        long id = body.get("userId").asLong();
        if (!"STUDENT".equals(role)) {
            sql("DELETE FROM user_roles WHERE user_id = ?", id);
            sql("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, role);
        }
        return new TestUser(id, email, password, client);
    }

    /** Signs in again with a fresh client (a new browser). */
    protected ApiClient login(String email, String password) {
        ApiClient client = anonymous();
        client.post("/api/auth/login", Map.of("email", email, "password", password)).expect(200);
        return client;
    }

    /** Creates a class through the admin API and returns its id. */
    protected long createClass(TestUser admin) {
        String name = "C" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
        admin.api().post("/api/admin/classes", Map.of("name", name)).ok();
        for (JsonNode c : admin.api().get("/api/classes/list").expect(200).json()) {
            if (name.equals(c.get("name").asText())) {
                return c.get("id").asLong();
            }
        }
        throw new AssertionError("class " + name + " was not listed");
    }

    /** Places a student in a class through the admin API. */
    protected void placeInClass(TestUser admin, TestUser student, long classId) {
        JsonNode me = student.api().get("/api/users/me").expect(200).json();
        admin.api()
                .put(
                        "/api/admin/students/" + student.id(),
                        Map.of(
                                "firstName", me.get("firstName").asText(),
                                "lastName", me.get("lastName").asText(),
                                "email", student.email(),
                                "classId", classId))
                .ok();
    }

    /**
     * Creates a course taught by {@code teacher}, gives it one module that is current today and
     * assigns it to {@code classId}. Returns the course code.
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
        String today = java.time.LocalDate.now().toString();
        String nextMonth = java.time.LocalDate.now().plusMonths(1).toString();
        Map<String, Object> module =
                Map.of(
                        "title",
                        "Module 1",
                        "description",
                        "First module",
                        "sortOrder",
                        1,
                        "startDate",
                        today,
                        "endDate",
                        nextMonth,
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
                                java.util.List.of(module),
                                "selectedGroupIds",
                                java.util.List.of(classId)))
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

    public record TestUser(long id, String email, String password, ApiClient api) {}

    public record Course(long id, String code, long moduleId) {}
}
