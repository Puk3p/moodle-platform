package moodlev2.application.auth;

import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.LoginService;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class LoginServiceImpl implements LoginService {
    private static final Duration ACCES_TOKEN_VALIDITY = Duration.ofHours(1);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;

    @Override
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email == null ? null : request.email.trim().toLowerCase();

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

        if (!passwordHasher.matches(request.password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid passwordddddd");
        }

        String accessToken = tokenService.generateToken(
                user,
                ACCES_TOKEN_VALIDITY,
                Set.of("access:api")
        );

        return new AuthResponse(
                accessToken,
                user.getId() != null ? user.getId().toString() : null,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }


}
