package moodlev2.infrastructure.persistence.jpa;

import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenEntity, Long> {
    Optional<PasswordResetTokenEntity> findByToken(String token);
}
