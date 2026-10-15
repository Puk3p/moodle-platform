package moodlev2.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Chat between teachers and students: the REST API, live delivery over WebSocket, and the
 * conversation history in its own database. Users, courses and quiz locks belong to identity-,
 * academic- and assessment-service, reached through their internal APIs ({@link
 * moodlev2.messaging.clients}) and their events on Kafka.
 */
@SpringBootApplication
public class MessagingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MessagingServiceApplication.class, args);
    }
}
