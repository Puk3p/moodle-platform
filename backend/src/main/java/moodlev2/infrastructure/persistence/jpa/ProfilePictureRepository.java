package moodlev2.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.ProfilePictureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProfilePictureRepository extends JpaRepository<ProfilePictureEntity, Long> {

    Optional<ProfilePictureEntity> findFirstByUserIdAndRetiredAtIsNull(Long userId);

    /** Current pictures whose account was deleted (user_id set to NULL by the foreign key). */
    List<ProfilePictureEntity> findByUserIdIsNullAndRetiredAtIsNull();

    /** Current pictures of deactivated accounts. */
    @Query(
            "SELECT p FROM ProfilePictureEntity p, UserEntity u "
                    + "WHERE p.userId = u.id AND u.active = false AND p.retiredAt IS NULL")
    List<ProfilePictureEntity> findCurrentOfInactiveUsers();

    List<ProfilePictureEntity> findByPurgeAfterBefore(Instant cutoff);

    boolean existsByStorageKey(String storageKey);
}
