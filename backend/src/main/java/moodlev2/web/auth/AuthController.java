package moodlev2.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.SessionService;
import moodlev2.application.auth.interfaces.ILoginService;
import moodlev2.application.auth.interfaces.ILoginService.LoginResult;
import moodlev2.application.auth.interfaces.IPasswordResetService;
import moodlev2.application.auth.interfaces.IRegisterService;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.security.SessionAuthenticationFilter;
import moodlev2.infrastructure.security.SessionCookies;
import moodlev2.web.auth.dto.AuthResponse;
import moodlev2.web.auth.dto.ForgotPasswordRequest;
import moodlev2.web.auth.dto.LoginRequest;
import moodlev2.web.auth.dto.RegisterRequest;
import moodlev2.web.auth.dto.ResetPasswordRequest;
import moodlev2.web.auth.dto.VerifyTwoFaLoginRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Sign-in, sign-out and session status. Every successful sign-in ends in an HttpOnly session cookie
 * set here; response bodies only ever describe the user, never carry a credential.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final ILoginService loginService;
    private final IRegisterService registerService;
    private final IPasswordResetService passwordResetService;
    private final SessionService sessionService;
    private final SessionCookies cookies;
    private final UserRepositoryPort users;

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResult result = loginService.login(request);

        if (result instanceof LoginResult.TwoFactorRequired challenge) {
            cookies.setChallenge(httpResponse, challenge.challengeToken());
            return AuthResponse.twoFactorRequired();
        }
        User user = ((LoginResult.Authenticated) result).user();
        startSession(user, httpRequest, httpResponse);
        return AuthResponse.of(user);
    }

    @PostMapping("/login/verify-2fa")
    public AuthResponse verifyTwoFaLogin(
            @RequestBody VerifyTwoFaLoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String challenge =
                cookies.readChallenge(httpRequest)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Your sign-in expired. Please sign in again."));

        User user = loginService.verifyTwoFaLogin(challenge, request.code());
        cookies.clearChallenge(httpResponse);
        startSession(user, httpRequest, httpResponse);
        return AuthResponse.of(user);
    }

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        User user = registerService.register(request);
        startSession(user, httpRequest, httpResponse);
        return AuthResponse.of(user);
    }

    /** Ends the session on the server, not just in the browser, and clears the cookie. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        if (httpRequest.getAttribute(SessionAuthenticationFilter.CURRENT_SESSION)
                instanceof SessionService.Resolved current) {
            sessionService.revokeByHash(current.tokenHash());
        }
        cookies.clearSession(httpResponse);
    }

    /** Who is signed in; the SPA calls this on start-up since it can no longer read a token. */
    @GetMapping("/session")
    public AuthResponse session(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return users.findByEmail(authentication.getName())
                .map(AuthResponse::of)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping("/forgot-password")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.processForgotPassword(request.getEmail());
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
    }

    /**
     * Issues a brand-new session. Any session this browser already had is revoked first, so a
     * session identifier planted before sign-in (session fixation) is never promoted.
     */
    private void startSession(User user, HttpServletRequest request, HttpServletResponse response) {
        cookies.readSession(request)
                .ifPresent(
                        old ->
                                sessionService.revokeByHash(
                                        moodlev2.common.util.TokenHashUtil.sha256(old)));
        SessionService.Issued issued =
                sessionService.create(
                        user.getId(),
                        request.getRemoteAddr(),
                        request.getHeader(HttpHeaders.USER_AGENT));
        cookies.setSession(response, issued.token(), issued.maxAge());
    }
}
