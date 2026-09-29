package moodlev2.application.auth.implementations;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.LoginAttemptGuard;
import moodlev2.application.auth.TwoFactorChallenges;
import moodlev2.application.auth.interfaces.ILoginService;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.web.auth.dto.LoginRequest;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class LoginService implements ILoginService {

    // A valid BCrypt hash of a random value, compared against when the account is not found so
    // that authentication timing is constant regardless of account existence.
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa8n8Q9m0m3p3vJ3n1qQ9k1s5m8n0uK";

    private static final String BAD_CREDENTIALS = "Invalid email or password";

    static final String PASSWORD = "password";
    static final String SECOND_FACTOR = "2fa";

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;
    private final TwoFactorService twoFactorService;
    private final TwoFactorChallenges challenges;
    private final LoginAttemptGuard attempts;

    @Override
    public LoginResult login(LoginRequest request) {
        String email = request.email == null ? "" : request.email.trim().toLowerCase();
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        attempts.checkAllowed(PASSWORD, email);

        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            // Same work and same answer as a wrong password, so neither timing nor message reveals
            // whether the account exists.
            passwordHasher.matches(request.password, DUMMY_HASH);
            attempts.recordFailure(PASSWORD, email);
            throw new IllegalArgumentException(BAD_CREDENTIALS);
        }

        User user = found.get();
        if (!passwordHasher.matches(request.password, user.getPasswordHash())) {
            attempts.recordFailure(PASSWORD, email);
            throw new IllegalArgumentException(BAD_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("User account is disabled");
        }
        attempts.recordSuccess(PASSWORD, email);

        if (user.isTwoFaEnabled()) {
            return new LoginResult.TwoFactorRequired(challenges.issue(user));
        }
        return new LoginResult.Authenticated(user);
    }

    @Override
    public User verifyTwoFaLogin(String challengeToken, String code) {
        Long userId = challenges.userIdFor(challengeToken);
        User user =
                userRepository
                        .findById(userId)
                        .filter(User::isEnabled)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Your sign-in expired. Please sign in again."));

        attempts.checkAllowed(SECOND_FACTOR, user.getEmail());
        if (!twoFactorService.verifyCode(user.getEmail(), code)) {
            challenges.recordFailure(challengeToken);
            attempts.recordFailure(SECOND_FACTOR, user.getEmail());
            throw new IllegalArgumentException("Invalid verification code.");
        }

        challenges.consume(challengeToken);
        attempts.recordSuccess(SECOND_FACTOR, user.getEmail());
        return user;
    }
}
