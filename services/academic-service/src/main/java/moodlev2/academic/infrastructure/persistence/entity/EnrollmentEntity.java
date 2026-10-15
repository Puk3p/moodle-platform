package moodlev2.academic.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class EnrollmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The enrolled account (identity module). */
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    private String status;
}
