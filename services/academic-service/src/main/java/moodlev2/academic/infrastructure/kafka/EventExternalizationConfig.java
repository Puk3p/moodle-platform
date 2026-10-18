package moodlev2.academic.infrastructure.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import moodlev2.academic.api.AssignmentPublishedEvent;
import moodlev2.academic.api.ModuleRemovedEvent;
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
 * such as {@code academic.ModuleRemoved}, never on a Java class name, so services do not share
 * classes. The key is the course id, so one course's events stay in order.
 */
@Configuration
public class EventExternalizationConfig {

    public static final String EVENT_TYPE_HEADER = "event-type";

    public static final String ACADEMIC_COURSE = "academic.course";
    public static final String ACADEMIC_COURSEWORK = "academic.coursework";

    private static final Set<Class<?>> EXTERNALIZED =
            Set.of(AssignmentPublishedEvent.class, ModuleRemovedEvent.class);

    @Bean
    EventExternalizationConfiguration eventExternalizationConfiguration() {
        return EventExternalizationConfiguration.externalizing()
                .selectByType(EXTERNALIZED::contains)
                .route(
                        ModuleRemovedEvent.class,
                        e ->
                                RoutingTarget.forTarget(ACADEMIC_COURSE)
                                        .andKey(String.valueOf(e.courseId())))
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
                    case ModuleRemovedEvent e -> "academic.ModuleRemoved";
                    case AssignmentPublishedEvent e -> "academic.AssignmentPublished";
                    default -> event.getClass().getSimpleName();
                };
        return type.getBytes(StandardCharsets.UTF_8);
    }

    @Bean
    NewTopic academicCourseTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(ACADEMIC_COURSE).partitions(partitions).build();
    }

    @Bean
    NewTopic academicCourseworkTopic(@Value("${app.kafka.partitions:3}") int partitions) {
        return TopicBuilder.name(ACADEMIC_COURSEWORK).partitions(partitions).build();
    }
}
