package moodlev2.infrastructure.persistence.jpa.entity;

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

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    private String title;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_type")
    private String eventType;

    private String description;
}
