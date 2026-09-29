package moodlev2.application.auth.implementations;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import moodlev2.application.auth.interfaces.ITwoFactorService;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TwoFactorService implements ITwoFactorService {

    private static final int PERIOD_SECONDS = 30;

    /** Accept the previous and next 30-second step too, for clock drift. */
    private static final int ALLOWED_DRIFT_STEPS = 1;

    private final SpringDataUserRepository userRepository;
    private final Clock clock;
    private final DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);

    /**
     * Last time step accepted per user. A code is single-use: anyone watching a user type it (or a
     * replayed request) cannot use it again inside its 30-90 second validity.
     */
    private final Map<Long, Long> lastAcceptedStep = new ConcurrentHashMap<>();

    @Autowired
    public TwoFactorService(SpringDataUserRepository userRepository) {
        this(userRepository, Clock.systemUTC());
    }

    TwoFactorService(SpringDataUserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    /**
     * Starts enrolment. Refused once 2FA is on: returning the existing secret would let anyone
     * holding a session copy the authenticator and defeat 2FA for good.
     */
    @Override
    @Transactional
    public TwoFactorSetupDto setupTwoFactor(String email) {
        UserEntity user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.isTwoFaEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Two-factor authentication is already enabled.");
        }

        if (user.getTwoFaSecret() == null) {
            SecretGenerator secretGenerator = new DefaultSecretGenerator();
            user.setTwoFaSecret(secretGenerator.generate());
            userRepository.save(user);
        }

        QrData data =
                new QrData.Builder()
                        .label(user.getEmail())
                        .secret(user.getTwoFaSecret())
                        .issuer("MoodleV2")
                        .algorithm(HashingAlgorithm.SHA1)
                        .digits(6)
                        .period(PERIOD_SECONDS)
                        .build();

        QrGenerator qrGenerator = new ZxingPngQrGenerator();
        String qrCodeImage;
        try {
            byte[] imageData = qrGenerator.generate(data);
            qrCodeImage = getDataUriForImage(imageData, qrGenerator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new IllegalStateException("Error generating QR code", e);
        }

        return new ITwoFactorService.TwoFactorSetupDto(user.getTwoFaSecret(), qrCodeImage);
    }

    @Override
    @Transactional
    public boolean verifyAndEnable(String email, String code) {
        UserEntity user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTwoFaSecret() == null) {
            throw new IllegalArgumentException("2FA not initialized");
        }

        boolean valid = check(user, code);
        if (valid) {
            user.setTwoFaEnabled(true);
            userRepository.save(user);
        }
        return valid;
    }

    @Override
    public boolean verifyCode(String email, String code) {
        return userRepository
                .findByEmail(email)
                .filter(u -> u.getTwoFaSecret() != null)
                .map(u -> check(u, code))
                .orElse(false);
    }

    private boolean check(UserEntity user, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long now = clock.instant().getEpochSecond() / PERIOD_SECONDS;
        byte[] given = code.getBytes(StandardCharsets.US_ASCII);

        for (long step = now - ALLOWED_DRIFT_STEPS; step <= now + ALLOWED_DRIFT_STEPS; step++) {
            String expected;
            try {
                expected = generator.generate(user.getTwoFaSecret(), step);
            } catch (CodeGenerationException e) {
                return false;
            }
            // Constant-time comparison.
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), given)) {
                final long matched = step;
                Long previous = lastAcceptedStep.get(user.getId());
                if (previous != null && matched <= previous) {
                    return false; // replay of an already-used code
                }
                lastAcceptedStep.put(user.getId(), matched);
                return true;
            }
        }
        return false;
    }
}
