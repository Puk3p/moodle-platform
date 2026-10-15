package moodlev2.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Quizzes, attempts, proctoring and the question bank, in their own database. Users and courses
 * belong to other services, reached through core's internal API ({@link moodlev2.assessment.core});
 * attempt changes are published to Kafka through a transactional outbox.
 */
@SpringBootApplication
public class AssessmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssessmentServiceApplication.class, args);
    }
}
