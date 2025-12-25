package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime; // <--- IMPORT NOU

@Entity
@Table(name = "module_items")
@Getter @Setter @NoArgsConstructor
public class ModuleItemEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private CourseModuleEntity module;

    private String title;
    private String type;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private String fileSize;

    private String url;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_assignment")
    private Boolean isAssignment = false;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "created_at")
    private Instant createdAt;
}