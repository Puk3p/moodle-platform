package moodlev2.web.auth;

import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.interfaces.ITwoFactorService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final ITwoFactorService twoFactorService;

    @PostMapping("/setup")
    public ITwoFactorService.TwoFactorSetupDto setup(Authentication authentication) {
        return twoFactorService.setupTwoFactor(authentication.getName());
    }

    @PostMapping("/verify")
    public boolean verify(@RequestBody VerifyCodeRequest request, Authentication authentication) {
        return twoFactorService.verifyAndEnable(authentication.getName(), request.code);
    }

    public record VerifyCodeRequest(String code) {}
}
