package moodlev2.messaging.infrastructure.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import moodlev2.messaging.application.chat.ChatHistoryMaintenance;
import moodlev2.messaging.application.chat.ChatService;
import moodlev2.messaging.infrastructure.kafka.CoreEvents.QuizAttemptChanged;
import moodlev2.messaging.infrastructure.kafka.CoreEvents.SessionsRevoked;
import moodlev2.messaging.infrastructure.kafka.CoreEvents.UserDeleted;
import moodlev2.messaging.infrastructure.kafka.CoreEvents.UserEmailChanged;
import moodlev2.messaging.infrastructure.websocket.WebSocketSessionRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Reacts to what happens in core.
 *
 * <p>Account changes are applied once per service (one consumer group). Live updates go to every
 * instance (a group per instance, starting at the latest offset), because each instance holds its
 * own WebSocket connections and only it can close or notify them.
 *
 * <p>A record that keeps failing is retried and then parked on {@code <topic>.DLT} (see {@link
 * KafkaConfig}) rather than blocking the partition.
 */
@Component
@RequiredArgsConstructor
class CoreEventListeners {

    private static final Logger log = LoggerFactory.getLogger(CoreEventListeners.class);

    private final ObjectMapper json;
    private final ChatHistoryMaintenance history;
    private final ChatService chat;
    private final WebSocketSessionRegistry sockets;

    @KafkaListener(topics = "identity.user", groupId = "${spring.application.name}")
    void onAccountChange(ConsumerRecord<String, String> record) throws JsonProcessingException {
        switch (CoreEvents.typeOf(record)) {
            case "identity.UserEmailChanged" -> {
                UserEmailChanged e = json.readValue(record.value(), UserEmailChanged.class);
                history.renameParticipant(e.oldEmail(), e.newEmail());
            }
            case "identity.UserDeleted" -> {
                UserDeleted e = json.readValue(record.value(), UserDeleted.class);
                history.forgetParticipant(e.email());
            }
            default -> log.debug("Ignoring {} on identity.user", CoreEvents.typeOf(record));
        }
    }

    @KafkaListener(
            topics = "identity.session",
            groupId = "${app.kafka.live-group}",
            properties = "auto.offset.reset=latest")
    void onSessionsRevoked(ConsumerRecord<String, String> record) throws JsonProcessingException {
        if ("identity.SessionsRevoked".equals(CoreEvents.typeOf(record))) {
            sockets.closeSessions(
                    json.readValue(record.value(), SessionsRevoked.class).tokenHashes());
        }
    }

    @KafkaListener(
            topics = "assessment.attempt",
            groupId = "${app.kafka.live-group}",
            properties = "auto.offset.reset=latest")
    void onQuizAttemptChanged(ConsumerRecord<String, String> record)
            throws JsonProcessingException {
        if ("assessment.QuizAttemptChanged".equals(CoreEvents.typeOf(record))) {
            QuizAttemptChanged e = json.readValue(record.value(), QuizAttemptChanged.class);
            if (e.blocksMessaging()) {
                chat.pushStatus(e.userEmail());
            }
        }
    }
}
