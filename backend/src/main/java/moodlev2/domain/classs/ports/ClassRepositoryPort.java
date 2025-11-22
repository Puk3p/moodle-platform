package moodlev2.domain.classs.ports;

import moodlev2.domain.classs.Class;

import java.util.Optional;

public interface ClassRepositoryPort {
    Optional<Class> findById(Long id);
    Optional<Class> findByName(String name);
}
