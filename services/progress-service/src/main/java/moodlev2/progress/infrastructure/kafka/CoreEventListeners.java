package moodlev2.progress.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import moodlev2.progress.application.calendar.DeadlineCalendar;
import moodlev2.progress.infrastructure.persistence.GradeRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * What progress needs to hear from the rest of the platform: assignment deadlines for the calendar,
 * and deleted accounts whose grades must go (the old database cascade). One consumer group for the
 * service, so each event is applied once; both handlers are idempotent.
 */
@Component
@RequiredArgsConstructor
class CoreEventListeners {

    private final ObjectMapper json;
    private final DeadlineCalendar deadlines;
    private final GradeRepository grades;

    @KafkaListener(topics = "academic.coursework", groupId = "${spring.application.name}")
    void onCoursework(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("academic.AssignmentPublished".equals(typeOf(record))) {
            AssignmentPublished e = json.readValue(record.value(), AssignmentPublished.class);
            deadlines.assignmentPublished(e.courseId(), e.assignmentId(), e.title(), e.dueDate());
        }
    }

    @KafkaListener(topics = "identity.user", groupId = "${spring.application.name}")
    void onAccountChange(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("identity.UserDeleted".equals(typeOf(record))) {
            grades.deleteByUserId(json.readValue(record.value(), UserDeleted.class).userId());
        }
    }

    private static String typeOf(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("event-type");
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }

    record AssignmentPublished(
            long courseId, long assignmentId, String title, LocalDateTime dueDate) {}

    record UserDeleted(long userId, String email) {}
}
