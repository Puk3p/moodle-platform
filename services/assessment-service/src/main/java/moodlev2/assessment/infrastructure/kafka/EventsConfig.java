package moodlev2.assessment.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import moodlev2.assessment.api.QuizAttemptChangedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Events this service publishes, and how it handles the ones it consumes.
 *
 * <p>Outbound, as in core: a {@link QuizAttemptChangedEvent} is recorded in EVENT_PUBLICATION with
 * the attempt and sent to {@code assessment.attempt} after commit, keyed by user, labelled {@code
 * assessment.QuizAttemptChanged}. messaging-service locks and unlocks chat on it.
 */
@Configuration
class EventsConfig {

    static final String ASSESSMENT_ATTEMPT = "assessment.attempt";

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(QuizAttemptChangedEvent.class::equals)
                .route(
                        QuizAttemptChangedEvent.class,
                        e ->
                                RoutingTarget.forTarget(ASSESSMENT_ATTEMPT)
                                        .andKey(String.valueOf(e.userId())))
                .headers(
                        event ->
                                Map.of(
                                        "event-type",
                                        "assessment.QuizAttemptChanged"
                                                .getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    @Bean
    NewTopic assessmentAttemptTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(ASSESSMENT_ATTEMPT).partitions(partitions).build();
    }

    /**
     * Retry with backoff, then park the record on {@code <topic>.DLT}. Consumed records are
     * strings, while the outbox producer sends bytes, so dead letters get a template of their own.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaProperties kafka) {
        Map<String, Object> props = new java.util.HashMap<>(kafka.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<Object, Object> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
        ExponentialBackOff backOff = new ExponentialBackOff(500, 2.0);
        backOff.setMaxElapsedTime(10_000);
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
        handler.addNotRetryableExceptions(com.fasterxml.jackson.core.JsonProcessingException.class);
        return handler;
    }
}
