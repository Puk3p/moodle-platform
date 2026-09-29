package moodlev2.application.auth.implementations;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.SessionService;
import moodlev2.application.auth.interfaces.IPasswordResetService;
import moodlev2.common.util.TokenHashUtil;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.infrastructure.persistence.jpa.PasswordResetTokenRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password reset by email link.
 *
 * <ul>
 *   <li>Tokens are 256-bit random and stored only as SHA-256, so a database leak yields no usable
 *       reset link. Each new request invalidates earlier links; a successful reset invalidates all.
 *   <li>A reset signs the account out everywhere: whoever prompted the reset may hold a session.
 *   <li>The response is identical whether or not the account exists, and the email is sent in the
 *       background so response time does not reveal it either.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService implements IPasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String INVALID = "This reset link is invalid or has expired.";

    private final SpringDataUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordHasherPort passwordHasher;
    private final SessionService sessionService;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public void processForgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        String normalized = email.trim().toLowerCase();

        userRepository
                .findByEmail(normalized)
                .ifPresent(
                        user -> {
                            tokenRepository.deleteAllForUser(user.getId());

                            byte[] bytes = new byte[32];
                            random.nextBytes(bytes);
                            String token =
                                    Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

                            PasswordResetTokenEntity row = new PasswordResetTokenEntity();
                            row.setToken(TokenHashUtil.sha256(token));
                            row.setUser(user);
                            row.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
                            tokenRepository.save(row);

                            String to = user.getEmail();
                            CompletableFuture.runAsync(() -> sendEmail(to, token));
                        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(INVALID);
        }
        PasswordResetTokenEntity resetToken =
                tokenRepository
                        .findByToken(TokenHashUtil.sha256(token))
                        .orElseThrow(() -> new IllegalArgumentException(INVALID));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException(INVALID);
        }

        moodlev2.common.util.PasswordPolicy.validate(newPassword);

        UserEntity user = resetToken.getUser();
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);

        tokenRepository.deleteAllForUser(user.getId());
        sessionService.revokeAll(user.getId());
    }

    public void sendEmail(String to, String token) {
        try {
            String link = frontendUrl + "/#/reset-password?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Reset Password - Moodle V2");
            message.setText(
                    "Click the link to reset your password: "
                            + link
                            + "\n\nThe link works once and expires in one hour. If you did not ask"
                            + " for this, you can ignore this email.");
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.warn("Password reset email could not be sent");
        }
    }
}
