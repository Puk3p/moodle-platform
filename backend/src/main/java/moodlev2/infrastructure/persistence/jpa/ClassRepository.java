package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByName(String name);
}
