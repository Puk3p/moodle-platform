package moodlev2.infrastructure.persistence.jpa;

import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenEntity, Long> {

    /**
     * @param tokenHash SHA-256 of the token from the email link; tokens are never stored raw
     */
    Optional<PasswordResetTokenEntity> findByToken(String tokenHash);

    @Modifying
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.user.id = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
