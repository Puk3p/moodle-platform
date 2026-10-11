package moodlev2.platform.config;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import moodlev2.academic.api.AssignmentPublishedEvent;
import moodlev2.assessment.api.QuizAttemptChangedEvent;
import moodlev2.identity.api.SessionsRevokedEvent;
import moodlev2.identity.api.UserDeletedEvent;
import moodlev2.identity.api.UserEmailChangedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.modulith.events.EventExternalizationConfiguration;
import org.springframework.modulith.events.RoutingTarget;

/**
 * Which in-process events leave this application, and where they go on Kafka.
 *
 * <p>Each event is recorded in the event publication registry (EVENT_PUBLICATION) in the same
 * transaction as the change that caused it and sent once that commits, so a crash between the two
 * cannot lose it: unfinished publications are sent again on restart.
 *
 * <p>Records are JSON. Consumers dispatch on the {@value #EVENT_TYPE_HEADER} header, a logical name
 * such as {@code identity.UserDeleted}, never on a Java class name, so services do not share
 * classes. The key is the id events must stay ordered by (the user, or the course).
 */
@Configuration
public class EventExternalizationConfig {

    public static final String EVENT_TYPE_HEADER = "event-type";

    public static final String IDENTITY_USER = "identity.user";
    public static final String IDENTITY_SESSION = "identity.session";
    public static final String ASSESSMENT_ATTEMPT = "assessment.attempt";
    public static final String ACADEMIC_COURSEWORK = "academic.coursework";

    private static final Set<Class<?>> EXTERNALIZED =
            Set.of(
                    UserEmailChangedEvent.class,
                    UserDeletedEvent.class,
                    SessionsRevokedEvent.class,
                    QuizAttemptChangedEvent.class,
                    AssignmentPublishedEvent.class);

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(EXTERNALIZED::contains)
                .route(
                        UserEmailChangedEvent.class,
                        e ->
                                RoutingTarget.forTarget(IDENTITY_USER)
                                        .andKey(String.valueOf(e.userId())))
                .route(
                        UserDeletedEvent.class,
                        e ->
                                RoutingTarget.forTarget(IDENTITY_USER)
                                        .andKey(String.valueOf(e.userId())))
                .route(
                        SessionsRevokedEvent.class,
                        e ->
                                RoutingTarget.forTarget(IDENTITY_SESSION)
                                        .andKey(String.valueOf(e.userId())))
                .route(
                        QuizAttemptChangedEvent.class,
                        e ->
                                RoutingTarget.forTarget(ASSESSMENT_ATTEMPT)
                                        .andKey(String.valueOf(e.userId())))
                .route(
                        AssignmentPublishedEvent.class,
                        e ->
                                RoutingTarget.forTarget(ACADEMIC_COURSEWORK)
                                        .andKey(String.valueOf(e.courseId())))
                .headers(event -> Map.of(EVENT_TYPE_HEADER, typeOf(event)))
                .build();
    }

    /** As raw bytes, so the header reaches consumers unencoded. */
    static byte[] typeOf(Object event) {
        String type =
                switch (event) {
                    case UserEmailChangedEvent e -> "identity.UserEmailChanged";
                    case UserDeletedEvent e -> "identity.UserDeleted";
                    case SessionsRevokedEvent e -> "identity.SessionsRevoked";
                    case QuizAttemptChangedEvent e -> "assessment.QuizAttemptChanged";
                    case AssignmentPublishedEvent e -> "academic.AssignmentPublished";
                    default -> event.getClass().getSimpleName();
                };
        return type.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    NewTopic identityUserTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(IDENTITY_USER).partitions(partitions).build();
    }

    @Bean
    NewTopic identitySessionTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(IDENTITY_SESSION).partitions(partitions).build();
    }

    @Bean
    NewTopic assessmentAttemptTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(ASSESSMENT_ATTEMPT).partitions(partitions).build();
    }

    @Bean
    NewTopic academicCourseworkTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(ACADEMIC_COURSEWORK).partitions(partitions).build();
    }
}
