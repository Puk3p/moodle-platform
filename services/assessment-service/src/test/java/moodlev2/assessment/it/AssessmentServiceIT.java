package moodlev2.assessment.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
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
 * assessment-service on its own: MySQL and Kafka in containers, identity- and academic-service
 * played by FakeServices.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssessmentServiceIT {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0").withDatabaseName("assessment");
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
    static void properties(DynamicPropertyRegistry registry) throws java.io.IOException {
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
        String uploads = java.nio.file.Files.createTempDirectory("assessment-it").toString();
        registry.add("app.uploads.dir", () -> uploads);
    }

    @AfterAll
    static void stopCore() {
        try {
            SERVICES.server.shutdown();
        } catch (java.io.IOException e) {
            // Going away anyway.
        }
    }

    private static final long TEACHER = 1, STUDENT = 2, OUTSIDER = 3;

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
        SERVICES.addUser(OUTSIDER, "outsider@it.test", "STUDENT", null);
        SERVICES.addCourse(10, "CS101", TEACHER, STUDENT);
    }

    @Test
    void aStudentTakesAMessagingBlockingQuizAndTheLockIsPublished() throws Exception {
        String title = "Quiz " + UUID.randomUUID();
        long quizId = createQuiz(title, true);

        // Authoring is for staff only.
        assertThat(
                        post(
                                        "/api/quizzes/create",
                                        Map.of("title", "x"),
                                        SERVICES.userToken(STUDENT))
                                .statusCode())
                .isEqualTo(403);
        // Someone outside the course cannot even learn the quiz exists.
        assertThat(
                        post(
                                        "/api/quizzes/" + quizId + "/start",
                                        Map.of(),
                                        SERVICES.userToken(OUTSIDER))
                                .statusCode())
                .isEqualTo(404);

        HttpResponse<String> started =
                post("/api/quizzes/" + quizId + "/start", Map.of(), SERVICES.userToken(STUDENT));
        assertThat(started.statusCode()).as(started.body()).isEqualTo(200);
        JsonNode attempt = json.readTree(started.body());
        assertThat(attempt.get("messagingBlocked").asBoolean()).isTrue();

        // messaging-service asks here while the quiz runs...
        assertThat(
                        json.readTree(
                                get(
                                                "/internal/exam-locks/users/" + STUDENT,
                                                SERVICES.serviceToken())
                                        .body()))
                .hasSize(1);
        // ...and hears about it from Kafka, through the outbox.
        ConsumerRecord<String, String> event =
                awaitRecord(
                        "assessment.attempt",
                        "assessment.QuizAttemptChanged",
                        String.valueOf(STUDENT),
                        "\"blocksMessaging\":true");
        assertThat(json.readTree(event.value()).get("blocksMessaging").asBoolean()).isTrue();

        JsonNode question = attempt.get("questions").get(0);
        long correct = -1;
        for (JsonNode option : question.get("options")) {
            if (option.get("text").asText().equals("4")) {
                correct = option.get("id").asLong();
            }
        }
        Map<String, Object> answer = new HashMap<>();
        answer.put("questionId", question.get("id").asLong());
        answer.put("selectedOptionId", correct);
        HttpResponse<String> submitted =
                post(
                        "/api/quizzes/submit",
                        Map.of(
                                "attemptId",
                                attempt.get("attemptId").asLong(),
                                "answers",
                                List.of(answer)),
                        SERVICES.userToken(STUDENT));
        assertThat(json.readTree(submitted.body()).get("passed").asBoolean()).isTrue();
        assertThat(
                        json.readTree(
                                get(
                                                "/internal/exam-locks/users/" + STUDENT,
                                                SERVICES.serviceToken())
                                        .body()))
                .isEmpty();

        // academic-service's course page asks for the course's quizzes; one attempt allowed, so
        // none left.
        JsonNode quizzes =
                json.readTree(
                        get(
                                        "/internal/quizzes/courses/10?viewerId=" + STUDENT,
                                        SERVICES.serviceToken())
                                .body());
        JsonNode mine = find(quizzes, "title", title);
        assertThat(mine.get("moduleId").asLong()).isEqualTo(20);
        assertThat(mine.get("canAttempt").asBoolean()).isFalse();
    }

    @Test
    void coreEventsUnfileQuizzesAndForgetDeletedAccounts() throws Exception {
        String title = "Unfiled " + UUID.randomUUID();
        long quizId = createQuiz(title, false);
        assertThat(
                        post(
                                        "/api/quizzes/" + quizId + "/start",
                                        Map.of(),
                                        SERVICES.userToken(STUDENT))
                                .statusCode())
                .isEqualTo(200);

        publish(
                "academic.course",
                "10",
                "academic.ModuleRemoved",
                Map.of("courseId", 10, "moduleId", 20));
        eventually(
                () ->
                        count(
                                        "SELECT COUNT(*) FROM quizzes WHERE id = "
                                                + quizId
                                                + " AND module_id IS NULL")
                                == 1);

        publish(
                "identity.user",
                String.valueOf(STUDENT),
                "identity.UserDeleted",
                Map.of("userId", STUDENT, "email", "student@it.test"));
        eventually(
                () -> count("SELECT COUNT(*) FROM quiz_attempts WHERE user_id = " + STUDENT) == 0);
    }

    @Test
    void aNewDatabaseStartsWithTheMonolithsSampleQuizzesAndQuestionBank() throws Exception {
        // The sample course CS201 (id 1 in academic-service) lists its published quiz only.
        HttpResponse<String> quizzes = get("/internal/quizzes/courses/1", SERVICES.serviceToken());
        assertThat(quizzes.statusCode()).isEqualTo(200);
        assertThat(quizzes.body())
                .contains("Complexity Basics")
                .doesNotContain("Random Weekly Quiz");

        assertThat(count("SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = 1")).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM quiz_options WHERE question_id IN (1, 2)"))
                .isEqualTo(6);
        // The sample student's (id 1 in identity-service) finished attempt, with its answers.
        assertThat(
                        count(
                                "SELECT COUNT(*) FROM quiz_attempts a JOIN quiz_responses r"
                                        + " ON r.attempt_id = a.id WHERE a.id = 1"
                                        + " AND a.user_id = 1 AND a.status = 'COMPLETED'"))
                .isEqualTo(2);
        // The draft has no access password: the monolith's V5 hash matched none, V6 cleared it.
        assertThat(count("SELECT COUNT(*) FROM quizzes WHERE id = 2 AND access_password IS NULL"))
                .isEqualTo(1);

        assertThat(count("SELECT COUNT(*) FROM categories WHERE id BETWEEN 1 AND 7")).isEqualTo(7);
        assertThat(count("SELECT COUNT(*) FROM questions WHERE id BETWEEN 1 AND 6")).isEqualTo(6);
        assertThat(count("SELECT COUNT(*) FROM question_tags WHERE question_id BETWEEN 1 AND 6"))
                .isEqualTo(9);
        assertThat(count("SELECT COUNT(*) FROM question_bank_options WHERE question_id IN (2, 3)"))
                .isEqualTo(6);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long createQuiz(String title, boolean blockMessaging) throws Exception {
        String teacher = SERVICES.userToken(TEACHER);
        String category = "Cat " + UUID.randomUUID();
        Map<String, Object> newCategory = new HashMap<>();
        newCategory.put("name", category);
        newCategory.put("parentId", null);
        assertThat(post("/api/question-bank/categories", newCategory, teacher).statusCode())
                .isEqualTo(200);
        long categoryId =
                find(
                                json.readTree(get("/api/question-bank/categories", teacher).body()),
                                "name",
                                category)
                        .get("id")
                        .asLong();

        String text = "2 + 2? " + UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);
        data.put("type", "SINGLE_CHOICE");
        data.put("difficulty", "EASY");
        data.put("categoryId", categoryId);
        data.put("tags", List.of("it"));
        data.put(
                "options",
                List.of(
                        Map.of("text", "3", "isCorrect", false),
                        Map.of("text", "4", "isCorrect", true)));
        assertThat(
                        multipartJson("/api/question-bank/questions", "data", data, teacher)
                                .statusCode())
                .isEqualTo(200);
        long questionId =
                find(
                                json.readTree(
                                        get(
                                                        "/api/question-bank/questions?categoryId="
                                                                + categoryId,
                                                        teacher)
                                                .body()),
                                "text",
                                text)
                        .get("id")
                        .asLong();

        Map<String, Object> quiz = new HashMap<>();
        quiz.put("title", title);
        quiz.put("courseId", 10);
        quiz.put("moduleId", 20);
        quiz.put("timeLimitMinutes", 30);
        quiz.put("passingScore", 50);
        quiz.put("maxAttempts", 1);
        quiz.put("generationType", "MANUAL");
        quiz.put("specificQuestionIds", List.of(questionId));
        quiz.put("blockMessaging", blockMessaging);
        HttpResponse<String> created = post("/api/quizzes/create", quiz, teacher);
        assertThat(created.statusCode()).as(created.body()).isEqualTo(200);

        JsonNode listed =
                find(json.readTree(get("/api/teacher/quizzes", teacher).body()), "title", title);
        assertThat(listed.get("courseName").asText()).isEqualTo("Course CS101");
        assertThat(listed.get("context").asText()).isEqualTo("Module 20");
        return listed.get("id").asLong();
    }

    private static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode node : array) {
            if (value.equals(node.path(field).asText())) {
                return node;
            }
        }
        throw new AssertionError("no " + field + "=" + value + " in " + array);
    }

    /** The first record of this type and key whose JSON contains {@code fragment}. */
    private ConsumerRecord<String, String> awaitRecord(
            String topic, String type, String key, String fragment) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plusSeconds(20);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    Header h = r.headers().lastHeader("event-type");
                    if (key.equals(r.key())
                            && r.value().contains(fragment)
                            && h != null
                            && type.equals(new String(h.value(), StandardCharsets.UTF_8))) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("no " + type + " on " + topic);
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
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Authorization", "Bearer " + bearer)
                        .build(),
                HttpResponse.BodyHandlers.ofString());
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

    private HttpResponse<String> multipartJson(
            String path, String part, Object value, String bearer) throws Exception {
        String boundary = "it-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(
                ("--"
                                + boundary
                                + "\r\nContent-Disposition: form-data; name=\""
                                + part
                                + "\"\r\nContent-Type: application/json\r\n\r\n")
                        .getBytes(StandardCharsets.UTF_8));
        body.write(json.writeValueAsBytes(value));
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Authorization", "Bearer " + bearer)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
