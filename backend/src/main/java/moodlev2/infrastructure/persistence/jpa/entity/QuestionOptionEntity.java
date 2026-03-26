package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_bank_options")
@Getter
@Setter
public class QuestionOptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    private String text;

    @Column(name = "is_correct")
    private boolean isCorrect;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
