package moodlev2.application.auth;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class LoginUseCase {
    private static final Duration ACCES_TOKEN_VALIDITY = Duration.ofHours(1);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;

    public Result login(String email, String rawPassword) {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();

        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }

        Optional<User> idkUser = userRepository.findByEmail(normalizedEmail);

        if (idkUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        User user = idkUser.get();
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }

        String accessToken = tokenService.generateToken(
                user,
                ACCES_TOKEN_VALIDITY,
                Set.of("access:api")
        );

        return new Result(user, accessToken);
    }





    public record Result(User user, String accesToken) {}
}
