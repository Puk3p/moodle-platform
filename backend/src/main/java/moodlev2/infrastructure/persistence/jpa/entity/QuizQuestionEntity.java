package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
@Getter @Setter @NoArgsConstructor
public class QuizQuestionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private QuizEntity quiz;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private String type;

    private Integer points;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuizOptionEntity> options = new ArrayList<>();

    public void addOption(QuizOptionEntity option) {
        options.add(option);
        option.setQuestion(this);
    }
}