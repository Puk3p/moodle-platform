package moodlev2.application.auth.interfaces;

public interface ITwoFactorService {
    record TwoFactorSetupDto(String secret, String qrImageBase64) {}

    TwoFactorSetupDto setupTwoFactor(String email);

    boolean verifyAndEnable(String email, String code);

    boolean verifyCode(String email, String code);
}
