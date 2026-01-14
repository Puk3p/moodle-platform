package moodlev2.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.implementations.LoginService;
import moodlev2.application.auth.implementations.RegisterService;
import moodlev2.application.auth.implementations.PasswordResetService;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import moodlev2.web.auth.dto.RegisterRequest;
import moodlev2.web.auth.dto.VerifyTwoFaLoginRequest;
import moodlev2.web.auth.dto.ForgotPasswordRequest;
import moodlev2.web.auth.dto.ResetPasswordRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginService loginService;
    private final RegisterService registerService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return loginService.login(request, ip, ua);
    }

    @PostMapping("/login/verify-2fa")
    public AuthResponse verifyTwoFaLogin(@RequestBody VerifyTwoFaLoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        String ua = httpRequest.getHeader("User-Agent");
        return loginService.verifyTwoFaLogin(request, ip, ua);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerService.register(request);
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

}