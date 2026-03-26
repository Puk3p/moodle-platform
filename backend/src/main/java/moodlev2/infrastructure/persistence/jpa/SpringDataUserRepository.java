package moodlev2.infrastructure.persistence.jpa;

import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
