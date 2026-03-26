package moodlev2.domain.user.ports;

import java.util.Optional;
import moodlev2.domain.user.User;

public interface UserRepositoryPort {
    Optional<User> findById(Long id); // cauta dupa id

    Optional<User> findByEmail(String email); // cauta dupa email

    boolean existsByEmail(String email); // verifica daca exista emailul

    User save(User user); // salveaza userul
}
