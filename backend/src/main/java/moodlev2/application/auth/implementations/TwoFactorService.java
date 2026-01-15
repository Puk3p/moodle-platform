package moodlev2.application.auth.implementations;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.ITwoFactorService;
import moodlev2.common.exception.NotFoundException;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

@Service
@RequiredArgsConstructor
public class TwoFactorService implements ITwoFactorService {
    private final SpringDataUserRepository userRepository;

    @Transactional
    public TwoFactorSetupDto setupTwoFactor(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTwoFaSecret() == null) {
            SecretGenerator secretGenerator = new DefaultSecretGenerator();
            String secret = secretGenerator.generate();
            user.setTwoFaSecret(secret);
            userRepository.save(user);
        }

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(user.getTwoFaSecret())
                .issuer("MoodleV2")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();

        String qrCodeImage;

        try {
            byte[] imageData = generator.generate(data);
            qrCodeImage = getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (QrGenerationException e) {
            throw new RuntimeException("Error generating QR code", e);
        }

        return new TwoFactorSetupDto(user.getTwoFaSecret(), qrCodeImage);
    }

    @Transactional
    public boolean verifyAndEnable(String email, String code) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTwoFaSecret() == null) {
            throw new IllegalArgumentException("2FA not initialized");
        }

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

        boolean isValid = codeVerifier.isValidCode(user.getTwoFaSecret(), code);

        if (isValid) {
            user.setTwoFaEnabled(true);
            userRepository.save(user);
        }

        return isValid;
    }

    public boolean verifyCode(String email, String code) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.getTwoFaSecret() == null) return false;

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

        return codeVerifier.isValidCode(user.getTwoFaSecret(), code);
    }

    public record TwoFactorSetupDto(String secret, String qrImageBase64) {}
}