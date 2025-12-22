package moodlev2.infrastructure.runner;

import lombok.RequiredArgsConstructor;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FixPasswordRunner implements CommandLineRunner {

    private final UserRepositoryPort userRepository;
    private final PasswordHasherPort passwordHasher;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- STARTED PASSWORD FIX ---");

        resetPasswordFor("student4@test.com", "password");

        resetPasswordFor("profesor@test.com", "password");

        System.out.println("--- FINISHED PASSWORD FIX ---");
    }

    private void resetPasswordFor(String email, String rawPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String cleanHash = passwordHasher.hash(rawPassword);

            user.setPasswordHash(cleanHash);
            user.setEnabled(true);

            userRepository.save(user);
            System.out.println("Password reset SUCCESS for: " + email + " -> Password: " + rawPassword);
        } else {
            System.out.println("User not found: " + email);
        }
    }
}