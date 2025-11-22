package moodlev2.web.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.implementations.LoginServiceImpl;
import moodlev2.application.auth.implementations.RegisterServiceImpl;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.LoginRequest;
import moodlev2.web.auth.dto.RegisterRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final LoginServiceImpl loginService;
    private final RegisterServiceImpl registerService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return loginService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerService.register(request);
    }
}