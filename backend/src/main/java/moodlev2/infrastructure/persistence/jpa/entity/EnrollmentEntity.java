package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "enrolments",
        uniqueConstraints = @UniqueConstraint(name = "uq_enrol_user_course", columnNames = {"user_id", "course_id"})
)
public class EnrollmentEntity {

    @Id
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
