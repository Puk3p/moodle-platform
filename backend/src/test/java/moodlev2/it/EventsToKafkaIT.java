package moodlev2.it;

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
 * Changes other services must hear about reach Kafka, keyed by user and labelled with a logical
 * event type, after the change commits.
 */
class EventsToKafkaIT extends IntegrationTestBase {

    @Test
    void anAdminEmailChangeIsPublishedOnIdentityUser() throws Exception {
        TestUser admin = admin();
        TestUser student = student();
        String newEmail = "evt-" + student.id() + "@it.test";
        JsonNode me = student.api().get("/api/users/me").expect(200).json();

        admin.api()
                .put(
                        "/api/admin/students/" + student.id(),
                        Map.of(
                                "firstName", me.get("firstName").asText(),
                                "lastName", me.get("lastName").asText(),
                                "email", newEmail))
                .ok();

        ConsumerRecord<String, String> record =
                awaitRecord(
                        "identity.user", "identity.UserEmailChanged", String.valueOf(student.id()));
        JsonNode payload = ApiClient.JSON.readTree(record.value());
        assertThat(payload.get("userId").asLong()).isEqualTo(student.id());
        assertThat(payload.get("oldEmail").asText()).isEqualTo(student.email());
        assertThat(payload.get("newEmail").asText()).isEqualTo(newEmail);

        // The same change also signed the student out everywhere.
        awaitRecord("identity.session", "identity.SessionsRevoked", String.valueOf(student.id()));
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
