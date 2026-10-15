package moodlev2.academic.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

/**
 * Course changes other services must hear about reach Kafka, keyed by course and labelled with a
 * logical event type, after the change commits.
 */
class EventsToKafkaIT extends IntegrationTestBase {

    @Test
    void anAssignmentWithADeadlineIsPublishedForTheCalendar() throws Exception {
        TestUser teacher = teacher();
        Course course = createCourse(teacher, createClass());
        Map<String, Object> parts = new java.util.LinkedHashMap<>();
        parts.put("courseCode", course.code());
        parts.put("moduleId", course.moduleId());
        parts.put("title", "Report " + course.code());
        parts.put("type", "Assignment");
        parts.put("isVisible", "true");
        parts.put("maxGrade", 10);
        parts.put("submissionType", "TEXT");
        parts.put("dueDate", java.time.LocalDateTime.now().plusDays(3).withNano(0).toString());
        teacher.api().multipart("POST", "/api/resources", parts).expect(200);

        ConsumerRecord<String, String> record =
                awaitRecord(
                        "academic.coursework",
                        "academic.AssignmentPublished",
                        String.valueOf(course.id()));
        JsonNode payload = ApiClient.JSON.readTree(record.value());
        assertThat(payload.get("title").asText()).isEqualTo("Report " + course.code());
        assertThat(payload.get("dueDate").asText()).isNotBlank();
    }

    @Test
    void removingAModuleIsPublishedSoItsQuizzesBecomeUnfiled() throws Exception {
        TestUser teacher = teacher();
        long classId = createClass();
        Course course = createCourse(teacher, classId);

        // Save the course with no modules: its one module is removed.
        teacher.api()
                .put(
                        "/api/courses/" + course.code(),
                        Map.of(
                                "id", course.id(),
                                "code", course.code(),
                                "title", "Course " + course.code(),
                                "term", "Fall 2026",
                                "status", "PUBLISHED",
                                "modules", List.of(),
                                "selectedGroupIds", List.of(classId)))
                .ok();

        ConsumerRecord<String, String> record =
                awaitRecord(
                        "academic.course", "academic.ModuleRemoved", String.valueOf(course.id()));
        assertThat(ApiClient.JSON.readTree(record.value()).get("moduleId").asLong())
                .isEqualTo(course.moduleId());
    }

    private ConsumerRecord<String, String> awaitRecord(String topic, String type, String key) {
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
}
