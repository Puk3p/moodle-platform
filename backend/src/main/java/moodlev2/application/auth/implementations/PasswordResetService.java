package moodlev2.application.auth.implementations;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.infrastructure.persistence.jpa.PasswordResetTokenRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.PasswordResetTokenEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final SpringDataUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordHasherPort passwordHasher;

    @Transactional
    public void processForgotPassword(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String token = UUID.randomUUID().toString();
        PasswordResetTokenEntity myToken = new PasswordResetTokenEntity();
        myToken.setToken(token);
        myToken.setUser(user);
        myToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS)); // expira intr o ora

        tokenRepository.save(myToken);

        sendEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token expired");
        }

        UserEntity user = resetToken.getUser();
        user.setPasswordHash(passwordHasher.hash(newPassword));
        userRepository.save(user);

        tokenRepository.delete(resetToken);
    }

    private void sendEmail(String to, String token) {
        String link = "http://localhost:4200/#/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Password - Moodle V2");
        message.setText("Click the link to reset your password: " + link);

        mailSender.send(message);
    }
}