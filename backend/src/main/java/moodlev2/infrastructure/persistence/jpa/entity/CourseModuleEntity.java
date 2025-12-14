package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "course_modules")
@Getter @Setter @NoArgsConstructor
public class CourseModuleEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private CourseEntity course;

    private String title;
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @OneToMany(mappedBy = "module", cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    private List<ModuleItemEntity> items = new ArrayList<>();
}