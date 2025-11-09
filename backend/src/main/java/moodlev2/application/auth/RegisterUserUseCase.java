package moodlev2.application.auth;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {
    private static final Duration ACCES_TOKEN_VALIDITY = Duration.ofHours(1);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;

    public Result register(String email,
                           String rawPassword,
                           String firstName,
                           String lastName) {
        String normalizedEmail;
        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        } else {
            normalizedEmail = email.trim().toLowerCase();
        }
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already in use");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(EnumSet.of(Role.STUDENT));
        user.setEnabled(true);

        User saved = userRepository.save(user);

        String accesToken = tokenService.generateToken(
                saved,
                ACCES_TOKEN_VALIDITY,
                Set.of("acces:api")
        );

        return new Result(saved, accesToken);

    }


    public record Result(User user, String accessToken) {}
}
