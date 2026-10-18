package moodlev2.identity.application.auth.implementations;

import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.Role;
import moodlev2.identity.application.auth.interfaces.IRegisterService;
import moodlev2.identity.domain.user.User;
import moodlev2.identity.domain.user.ports.PasswordHasherPort;
import moodlev2.identity.domain.user.ports.UserRepositoryPort;
import moodlev2.identity.web.auth.dto.RegisterRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RegisterService implements IRegisterService {
    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    /** Creates a student account. The caller starts the session; no token is returned here. */
    @Override
    public User register(RegisterRequest request) {
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

        moodlev2.identity.shared.PasswordPolicy.validate(request.password);

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordHasher.hash(request.password));
        user.setFirstName(request.firstName);
        user.setLastName(request.lastName);
        // Self-registration always yields a student; roles are never taken from the request.
        user.setRoles(EnumSet.of(Role.STUDENT));
        user.setEnabled(true);

        return userRepository.save(user);
    }
}
