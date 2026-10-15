package moodlev2.assessment.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import moodlev2.assessment.application.quiz.QuizHousekeeping;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * What used to be database cascades from the monolith's tables, now driven by identity-service's
 * and academic-service's events. One consumer group for the service; both handlers are idempotent.
 */
@Component
@RequiredArgsConstructor
class IncomingEventListeners {

    private final ObjectMapper json;
    private final QuizHousekeeping housekeeping;

    /** A deleted account's attempts go with it (they cascaded from users before). */
    @KafkaListener(topics = "identity.user", groupId = "${spring.application.name}")
    void onAccountChange(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("identity.UserDeleted".equals(typeOf(record))) {
            housekeeping.forgetUser(json.readTree(record.value()).path("userId").asLong());
        }
    }

    /** A removed module leaves its quizzes unfiled (module_id was ON DELETE SET NULL before). */
    @KafkaListener(topics = "academic.course", groupId = "${spring.application.name}")
    void onCourseChange(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("academic.ModuleRemoved".equals(typeOf(record))) {
            housekeeping.unfileModule(json.readTree(record.value()).path("moduleId").asLong());
        }
    }

    private static String typeOf(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("event-type");
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
