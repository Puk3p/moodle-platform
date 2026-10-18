package moodlev2.academic.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.application.resource.AccountHousekeeping;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** identity-service's account events; one consumer group for the service. */
@Component
@RequiredArgsConstructor
class IdentityEventListeners {

    private final ObjectMapper json;
    private final AccountHousekeeping housekeeping;

    /** A deleted account takes its enrollments and submissions with it. */
    @KafkaListener(topics = "identity.user", groupId = "${spring.application.name}")
    void onAccountChange(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("identity.UserDeleted".equals(typeOf(record))) {
            housekeeping.forgetUser(json.readTree(record.value()).path("userId").asLong());
        }
    }

    private static String typeOf(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("event-type");
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
