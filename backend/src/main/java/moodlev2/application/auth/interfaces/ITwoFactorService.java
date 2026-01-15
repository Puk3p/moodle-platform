package moodlev2.application.auth.interfaces;

import moodlev2.application.auth.implementations.TwoFactorService;

public interface ITwoFactorService {
    TwoFactorService.TwoFactorSetupDto setupTwoFactor(String email);
    boolean verifyAndEnable(String email, String code);
    boolean verifyCode(String email, String code);

}
