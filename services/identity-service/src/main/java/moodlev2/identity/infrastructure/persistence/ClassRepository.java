package moodlev2.identity.infrastructure.persistence;

import java.util.Optional;
import moodlev2.identity.infrastructure.persistence.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByName(String name);
}
