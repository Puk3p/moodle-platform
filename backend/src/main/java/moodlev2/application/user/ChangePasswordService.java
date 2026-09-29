package moodlev2.application.user;

import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.ITwoFactorService;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.user.dto.ChangePasswordRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final ITwoFactorService twoFactorService;
    private final moodlev2.application.auth.SessionService sessionService;
    private final moodlev2.application.auth.LoginAttemptGuard attempts;
    private final moodlev2.infrastructure.persistence.jpa.PasswordResetTokenRepository resetTokens;

    private static final String KIND = "change-password";

    /**
     * Changes the password and signs out every other session: if the old password leaked, whoever
     * used it loses access now. The session making the request stays signed in.
     */
    @Transactional
    public void changePassword(
            String email, ChangePasswordRequest request, String currentSessionHash) {
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        // Whoever holds a session must not get unlimited guesses at the password (or 2FA code) that
        // protects the account.
        attempts.checkAllowed(KIND, email);

        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            attempts.recordFailure(KIND, email);
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (user.isTwoFaEnabled()) {

            if (request.twoFaCode() == null || request.twoFaCode().isBlank()) {
                throw new IllegalArgumentException(
                        "2FA Code is required to change password because 2FA is enabled on your account.");
            }

            boolean isCodeValid = twoFactorService.verifyCode(user.getEmail(), request.twoFaCode());

            if (!isCodeValid) {
                attempts.recordFailure(KIND, email);
                throw new IllegalArgumentException("Invalid 2FA Code.");
            }
        }

        moodlev2.common.util.PasswordPolicy.validate(request.newPassword());

        String newHash = passwordHasher.hash(request.newPassword());
        user.setPasswordHash(newHash);

        userRepository.save(user);

        attempts.recordSuccess(KIND, email);
        // Outstanding reset links would otherwise still set a new password.
        resetTokens.deleteAllForUser(user.getId());
        sessionService.revokeOthers(user.getId(), currentSessionHash);
    }
}
