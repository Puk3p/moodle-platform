package moodlev2.progress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Grades and course calendars, in their own database. Users and courses belong to identity-service
 * and academic-service, reached through their internal APIs ({@link moodlev2.progress.clients});
 * deadlines arrive as events when teachers publish assignments.
 */
@SpringBootApplication
public class ProgressServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgressServiceApplication.class, args);
    }
}
