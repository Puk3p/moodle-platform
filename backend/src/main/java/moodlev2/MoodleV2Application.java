package moodlev2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoodleV2Application {

    public static void main(String[] args) {
        SpringApplication.run(MoodleV2Application.class, args);
    }

    // CORS is configured centrally in CorsConfig and applied through Spring Security so that a
    // single, environment-driven allow-list governs every request. A second wildcard MVC-level
    // CORS mapping used to live here; it was removed because it silently re-opened the API to any
    // origin and overrode the secure configuration.
}
