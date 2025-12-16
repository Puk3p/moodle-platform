package moodlev2.application.user;

import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.implementations.TwoFactorService;
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
    private final TwoFactorService twoFactorService;

    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (user.isTwoFaEnabled()) {

            if (request.twoFaCode() == null || request.twoFaCode().isBlank()) {
                throw new IllegalArgumentException("2FA Code is required to change password because 2FA is enabled on your account.");
            }

            boolean isCodeValid = twoFactorService.verifyCode(user.getEmail(), request.twoFaCode());

            if (!isCodeValid) {
                throw new IllegalArgumentException("Invalid 2FA Code.");
            }
        }

        String newHash = passwordHasher.hash(request.newPassword());
        user.setPasswordHash(newHash);

        userRepository.save(user);
    }
}