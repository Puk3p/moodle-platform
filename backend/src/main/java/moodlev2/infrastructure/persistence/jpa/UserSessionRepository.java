package moodlev2.infrastructure.persistence.jpa;

import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Long> {
    List<UserSessionEntity> findAllByUserEmail(String email);

    @Transactional
    void deleteByIdAndUserEmail(Long id, String email);

    @Transactional
    void deleteByUserEmailAndTokenSignatureNot(String email, String currentTokenSignature);
}