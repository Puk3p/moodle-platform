package moodlev2.academic.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moodlev2.assessment.infrastructure.persistence.entity.QuizEntity;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class CourseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String term;

    /** The teaching account (identity module); {@code null} when none is assigned. */
    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "image_url")
    private String imageUrl;

    private String status;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CourseModuleEntity> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<AnnouncementEntity> announcements = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<EnrollmentEntity> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    private List<QuizEntity> quizzes = new ArrayList<>();

    @Column(name = "created_at")
    private Instant createdAt;

    /** Classes (identity module) whose students all belong to this course. */
    @ElementCollection
    @CollectionTable(name = "course_classes", joinColumns = @JoinColumn(name = "course_id"))
    @Column(name = "class_id")
    private Set<Long> classIds = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
