package moodlev2.identity.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    List<UserEntity> findAllByClazzIdIn(Collection<Long> classIds);

    List<UserEntity> findAllByEmailIn(Collection<String> emails);
}
