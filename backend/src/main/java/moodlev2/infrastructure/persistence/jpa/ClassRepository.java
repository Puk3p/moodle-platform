package moodlev2.infrastructure.persistence.jpa;

import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByName(String name);
}
