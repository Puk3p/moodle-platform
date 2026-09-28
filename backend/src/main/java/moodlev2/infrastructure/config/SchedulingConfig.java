package moodlev2.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} jobs (currently the profile picture retention pass). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
