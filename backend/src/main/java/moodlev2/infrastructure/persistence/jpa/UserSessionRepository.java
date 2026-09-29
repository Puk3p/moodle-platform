package moodlev2.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {

    /** The per-request lookup: the session and its user (roles are eager) in one query. */
    @Query("SELECT s FROM UserSessionEntity s JOIN FETCH s.user WHERE s.tokenSignature = :hash")
    Optional<UserSessionEntity> findWithUserByTokenSignature(@Param("hash") String tokenHash);

    List<UserSessionEntity> findAllByUserEmail(String email);

    List<UserSessionEntity> findAllByUserIdOrderByCreatedAtAsc(Long userId);
}
