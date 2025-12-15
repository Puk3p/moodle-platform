package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "grades")
@Getter @Setter @NoArgsConstructor
public class GradeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

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