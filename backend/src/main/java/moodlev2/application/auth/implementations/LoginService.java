package moodlev2.application.auth.implementations;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.ILoginService; // Asigura-te ca interfata are metoda cu 3 parametri
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class LoginService implements ILoginService {
    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofHours(1);

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;

    private final UserSessionRepository userSessionRepository;
    private final SpringDataUserRepository jpaUserRepository;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
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
            throw new IllegalArgumentException("Invalid password");
        }

        String accessToken = tokenService.generateToken(
                user,
                ACCESS_TOKEN_VALIDITY,
                Set.of("access:api")
        );

        saveUserSession(user.getEmail(), accessToken, ipAddress, userAgent);

        return new AuthResponse(
                accessToken,
                user.getId() != null ? user.getId().toString() : null,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles()
        );
    }

    private void saveUserSession(String email, String accessToken, String ipAddress, String userAgent) {
        var userEntity = jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User entity not found for session saving"));

        UserSessionEntity session = new UserSessionEntity();
        session.setUser(userEntity);
        session.setIpAddress(ipAddress);
        session.setDeviceName(parseUserAgent(userAgent));


        String signature = accessToken.length() > 15
                ? accessToken.substring(accessToken.length() - 15)
                : accessToken;

        session.setTokenSignature(signature);

        userSessionRepository.save(session);
    }

    private String parseUserAgent(String ua) {
        if (ua == null) return "Unknown Device";

        String os = "Unknown OS";
        if (ua.contains("Windows")) os = "Windows";
        else if (ua.contains("Mac")) os = "macOS";
        else if (ua.contains("Linux")) os = "Linux";
        else if (ua.contains("Android")) os = "Android";
        else if (ua.contains("iPhone") || ua.contains("iPad")) os = "iOS";

        String browser = "Unknown Browser";
        if (ua.contains("Chrome") && !ua.contains("Edg")) browser = "Chrome";
        else if (ua.contains("Firefox")) browser = "Firefox";
        else if (ua.contains("Safari") && !ua.contains("Chrome")) browser = "Safari";
        else if (ua.contains("Edg")) browser = "Edge";

        return os + " · " + browser;
    }
}