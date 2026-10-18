package moodlev2.assessment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Quizzes, attempts, proctoring and the question bank, in their own database. Users and courses
 * belong to identity-service and academic-service, reached through their internal APIs ({@link
 * moodlev2.assessment.clients}); attempt changes are published to Kafka through a transactional
 * outbox.
 */
@SpringBootApplication
public class AssessmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssessmentServiceApplication.class, args);
    }
}
