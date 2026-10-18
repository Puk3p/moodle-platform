package moodlev2.gateway.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Drops cached tokens the moment identity-service ends a session or changes an account, so logging
 * out or being signed out by an admin takes effect immediately, not after the cache TTL.
 *
 * <p>Each gateway instance has its own cache, so each listens in a consumer group of its own, from
 * the latest offset (older events concern tokens it cannot have cached).
 */
@Component
class SessionEventsListener {

    private final ObjectMapper json;
    private final SessionResolver sessions;

    SessionEventsListener(ObjectMapper json, SessionResolver sessions) {
        this.json = json;
        this.sessions = sessions;
    }

    @KafkaListener(
            topics = {"identity.session", "identity.user"},
            groupId = "${app.kafka.instance-group}",
            properties = "auto.offset.reset=latest")
    void on(ConsumerRecord<String, String> record) throws JsonProcessingException {
        JsonNode event = json.readTree(record.value());
        switch (type(record)) {
            case "identity.SessionsRevoked" -> {
                List<String> hashes = new ArrayList<>();
                event.path("tokenHashes").forEach(h -> hashes.add(h.asText()));
                sessions.evictSessions(hashes);
            }
            case "identity.UserEmailChanged", "identity.UserDeleted" ->
                    sessions.evictUser(event.path("userId").asLong());
            default -> {
                // Not about sessions.
            }
        }
    }

    private static String type(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("event-type");
        return header == null ? "" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
