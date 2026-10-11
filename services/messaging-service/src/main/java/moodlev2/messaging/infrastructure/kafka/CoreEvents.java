package moodlev2.messaging.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

/**
 * Events core publishes that messaging acts on. Core sends JSON with a logical type in the {@code
 * event-type} header; these records are messaging's own reading of them, so the two never share
 * classes. Unknown fields are ignored, so core can add some.
 */
final class CoreEvents {

    static final String TYPE_HEADER = "event-type";

    private CoreEvents() {}

    static String typeOf(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader(TYPE_HEADER);
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }

    /** {@code identity.UserEmailChanged} on {@code identity.user}. */
    record UserEmailChanged(long userId, String oldEmail, String newEmail) {}

    /** {@code identity.UserDeleted} on {@code identity.user}. */
    record UserDeleted(long userId, String email) {}

    /** {@code identity.SessionsRevoked} on {@code identity.session}. */
    record SessionsRevoked(long userId, List<String> tokenHashes) {}

    /** {@code assessment.QuizAttemptChanged} on {@code assessment.attempt}. */
    record QuizAttemptChanged(long userId, String userEmail, boolean blocksMessaging) {}
}
