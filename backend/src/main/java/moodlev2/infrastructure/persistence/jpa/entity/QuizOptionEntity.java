package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quiz_options")
@Getter @Setter @NoArgsConstructor
public class QuizOptionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuizQuestionEntity question;

    @Column(nullable = false)
    private String text;

    @Column(name = "is_correct")
    private boolean isCorrect;

    @Column(name = "sort_order")
    private Integer sortOrder;
}