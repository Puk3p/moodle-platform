package moodlev2.academic.infrastructure.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/** How events from other services are consumed when handling one fails. */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Retry with backoff, then park the record on {@code <topic>.DLT}. Consumed records are
     * strings, while the outbox producer sends bytes, so dead letters get a template of their own.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaProperties kafka) {
        Map<String, Object> props = new HashMap<>(kafka.buildProducerProperties(null));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<Object, Object> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
        ExponentialBackOff backOff = new ExponentialBackOff(500, 2.0);
        backOff.setMaxElapsedTime(10_000);
        DefaultErrorHandler handler =
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), backOff);
        handler.addNotRetryableExceptions(JsonProcessingException.class);
        return handler;
    }
}
