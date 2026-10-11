package moodlev2.progress.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The course (academic module) this entry belongs to. */
    @Column(name = "course_id")
    private Long courseId;

    private String title;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_type")
    private String eventType;

    private String description;

    /** What produced the entry ({@code assignment}), so a repeated event does not duplicate it. */
    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;
}
