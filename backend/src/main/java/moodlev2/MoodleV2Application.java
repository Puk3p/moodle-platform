package moodlev2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoodleV2Application {

    public static void main(String[] args) {
        SpringApplication.run(MoodleV2Application.class, args);
    }

    // CORS is answered by the gateway, from one environment-driven allow-list, for every service.
    // A wildcard MVC-level CORS mapping used to live here; it was removed because it silently
    // re-opened the API to any origin.
}
