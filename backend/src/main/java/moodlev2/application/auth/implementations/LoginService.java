package moodlev2.application.auth.implementations;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.ILoginService;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import moodlev2.web.auth.dto.VerifyTwoFaLoginRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class LoginService implements ILoginService {
    private static final Duration ACCESS_TOKEN_VALIDITY = Duration.ofHours(1);
    private static final Duration TEMP_TOKEN_VALIDITY = Duration.ofMinutes(5); 

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TokenServicePort tokenService;
    private final TwoFactorService twoFactorService; 

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

        
        if (user.isTwoFaEnabled()) {
            
            
            User tempUser = new User();
            tempUser.setId(user.getId());
            tempUser.setEmail(user.getEmail());
            tempUser.setFirstName(user.getFirstName());
            tempUser.setLastName(user.getLastName());
            tempUser.setRoles(Set.of(Role.STUDENT));

            String tempToken = tokenService.generateToken(
                    tempUser,
                    TEMP_TOKEN_VALIDITY,
                    Set.of("auth:pre-2fa") 
            );

            
            return new AuthResponse(
                    tempToken,
                    null, null, null, null, null,
                    true 
            );
        }

        
        return finalizeLogin(user, ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse verifyTwoFaLogin(VerifyTwoFaLoginRequest request, String ipAddress, String userAgent) {
        
        TokenServicePort.TokenPayload payload;
        try {
            payload = tokenService.parse(request.tempToken());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired login session.");
        }

        
        User user = userRepository.findById(payload.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        
        boolean isValid = twoFactorService.verifyCode(user.getEmail(), request.code());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid 2FA Code");
        }

        
        return finalizeLogin(user, ipAddress, userAgent);
    }

    private AuthResponse finalizeLogin(User user, String ipAddress, String userAgent) {
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
                user.getRoles(),
                false 
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
        
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Unknown Device";
    }
}