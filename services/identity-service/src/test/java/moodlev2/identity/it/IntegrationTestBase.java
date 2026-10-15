package moodlev2.identity.it;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.kafka.KafkaContainer;

/**
 * Boots identity-service on a random port against a throwaway MySQL (Flyway migrates and seeds it)
 * and Kafka, and drives it over real HTTP through the full security chain, the way the gateway
 * passes browser requests through.
 *
 * <p>The containers are started once per JVM and shared by every IT class. Tests never rely on a
 * clean database: each one creates its own users and classes with unique names.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("identity")
                    .withUsername("identity")
                    .withPassword("identity");

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.9.1");

    static {
        MYSQL.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);

        byte[] secret = new byte[48];
        new SecureRandom().nextBytes(secret);
        String jwtSecret = Base64.getEncoder().encodeToString(secret);
        registry.add("security.jwt.secret", () -> jwtSecret);

        registry.add("app.session.cookie-secure", () -> "false");
        registry.add("app.internal.clients.it-service", () -> "it-service-secret");
        Path pictures = Files.createTempDirectory("identity-it-pictures");
        registry.add("app.profile-pictures.dir", pictures::toString);

        for (String provider : new String[] {"google", "facebook"}) {
            String prefix = "spring.security.oauth2.client.registration." + provider;
            registry.add(prefix + ".client-id", () -> "it-" + provider);
            registry.add(prefix + ".client-secret", () -> "it-secret");
        }
        registry.add("spring.mail.username", () -> "it@example.test");
        registry.add("spring.mail.password", () -> "unused");
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> "1");
    }

    @LocalServerPort int port;

    @Autowired JdbcTemplate jdbc;

    protected ApiClient anonymous() {
        return new ApiClient("http://127.0.0.1:" + port);
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
            jdbc.update("DELETE FROM user_roles WHERE user_id = ?", id);
            jdbc.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", id, role);
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
        JsonNode me = student.api().get("/api/auth/session").expect(200).json();
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

    /** The first record on {@code topic} with this key and logical event type. */
    protected ConsumerRecord<String, String> awaitRecord(String topic, String type, String key) {
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
                    Header header = r.headers().lastHeader("event-type");
                    if (key.equals(r.key())
                            && header != null
                            && type.equals(new String(header.value(), StandardCharsets.UTF_8))) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError("no " + type + " for key " + key + " on " + topic);
    }

    public record TestUser(long id, String email, String password, ApiClient api) {}
}
