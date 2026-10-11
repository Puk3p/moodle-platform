package moodlev2.identity.infrastructure.persistence;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.domain.user.User;
import moodlev2.identity.domain.user.ports.UserRepositoryPort;
import moodlev2.identity.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return springDataUserRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return springDataUserRepository.findByEmail(email).map(userMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        var entity = userMapper.toEntity(user);
        var savedEntity = springDataUserRepository.save(entity);
        return userMapper.toDomain(savedEntity);
    }
}
