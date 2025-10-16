package moodlev2;

import org.springframework.boot.SpringApplication;

public class TestMoodleV2Application {

    public static void main(String[] args) {
        SpringApplication.from(MoodleV2Application::main).with(TestcontainersConfiguration.class).run(args);
    }

}
