package moodlev2.progress.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;

@Entity
@Table(name = "grades")
@Getter
@Setter
@NoArgsConstructor
public class GradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The graded account (identity module). */
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "score_received")
    private BigDecimal scoreReceived;

    @Column(name = "max_score")
    private BigDecimal maxScore;

    @Column(name = "weight_label")
    private String weightLabel;

    @Column(name = "graded_at")
    private LocalDate gradedAt;

    @Column(name = "type_icon")
    private String typeIcon;
}
