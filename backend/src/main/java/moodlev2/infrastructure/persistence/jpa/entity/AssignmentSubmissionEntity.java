package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "assignment_submissions")
@Data
public class AssignmentSubmissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "assignment_id", nullable = false)
    private ModuleItemEntity assignment;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private UserEntity student;

    @Column(columnDefinition = "TEXT")
    private String textResponse;

    private String fileUrl;
    private String fileName;

    private LocalDateTime submittedAt;

    private Integer grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
