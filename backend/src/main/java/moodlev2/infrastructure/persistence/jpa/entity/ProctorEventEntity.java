package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single proctoring signal recorded during a quiz attempt (tab hidden, window blurred, fullscreen
 * exited, copy/paste). The quiz page cannot observe other tabs or sites; these events only record
 * that the student left the exam surface, and are disclosed to them.
 */
@Entity
@Table(name = "proctor_events")
@Getter
@Setter
@NoArgsConstructor
public class ProctorEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id")
    private QuizAttemptEntity attempt;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(length = 255)
    private String detail;

    @Column(name = "client_ts")
    private Long clientTs;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
