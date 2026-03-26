package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "classes")
@Getter
@Setter
@Entity
@NoArgsConstructor
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String name;

    @ManyToMany(mappedBy = "assignedClasses")
    private Set<CourseEntity> courses = new HashSet<>();
}
