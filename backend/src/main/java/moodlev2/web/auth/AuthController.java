package moodlev2.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.implementations.LoginService;
import moodlev2.application.auth.implementations.RegisterService;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import moodlev2.web.auth.dto.RegisterRequest;
import moodlev2.web.auth.dto.VerifyTwoFaLoginRequest;
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
}