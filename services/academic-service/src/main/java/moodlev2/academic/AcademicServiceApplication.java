package moodlev2.academic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Courses, their modules and resources, assignments and submissions, announcements, and who belongs
 * to which course. Accounts come from identity-service ({@link
 * moodlev2.academic.directory.DirectoryApi}); course changes are published to Kafka through a
 * transactional outbox.
 */
@SpringBootApplication
public class AcademicServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AcademicServiceApplication.class, args);
    }
}
