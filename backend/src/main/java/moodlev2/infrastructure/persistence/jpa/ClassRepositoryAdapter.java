package moodlev2.infrastructure.persistence.jpa;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.classs.Class;
import moodlev2.infrastructure.mapper.ClassMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClassRepositoryAdapter implements ClassRepository {
    private final ClassRepository classRepository;
    private final ClassMapper classMapper;

    @Override
    public Optional<Class> findById(Long id) {
        return classRepository.findById(id)
                .map(classMapper::toDomain);
    }
}
