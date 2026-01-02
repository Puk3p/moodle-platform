package moodlev2.application.auth.implementations;

import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.IRegisterService;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.RegisterRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class RegisterService implements IRegisterService {

    private static final Duration ACCES_TOKEN_VALIDITY = Duration.ofHours(1);

    private final UserRepositoryPort userRepository;
    private final TokenServicePort tokenService;
    private final PasswordHasherPort passwordHasher;


    @Override
    public AuthResponse register(RegisterRequest request) {
        String email = request.email;
        String normalizedEmail;

        if (email == null) {
            throw new IllegalArgumentException("Email cannot be null");
        } else {
            normalizedEmail = email.trim().toLowerCase();
        }

        if (normalizedEmail.isEmpty()) {
            throw new IllegalArgumentException("Emails cannot be empty");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email exists already in use.");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.hash(request.password));
        user.setFirstName(request.firstName);
        user.setLastName(request.lastName);
        user.setRoles(EnumSet.of(Role.STUDENT));
        user.setEnabled(true);

        User saved = userRepository.save(user);

        String accesToken = tokenService.generateToken(
                saved,
                ACCES_TOKEN_VALIDITY,
                Set.of("access:api")
        );

        return new AuthResponse(
                accesToken,
                saved.getId() != null ? saved.getId().toString() : null,
                saved.getEmail(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getRoles(),
                false
        );
    }
}
