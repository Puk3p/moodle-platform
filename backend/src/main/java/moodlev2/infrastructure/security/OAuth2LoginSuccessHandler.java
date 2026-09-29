package moodlev2.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import moodlev2.application.auth.SessionService;
import moodlev2.application.auth.TwoFactorChallenges;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * Finishes a Google/Facebook sign-in.
 *
 * <ul>
 *   <li>No credential ever appears in the redirect URL (it used to carry the JWT in the fragment,
 *       where it lands in browser history): the session is an HttpOnly cookie set on this response.
 *   <li>An account is matched by email only when the provider has verified that email; otherwise
 *       anyone able to register an unverified address at the provider could take over the local
 *       account with that address.
 *   <li>Disabled accounts are refused, and accounts with 2FA still have to enter their code: the
 *       browser gets the same pre-2FA challenge cookie as a password sign-in.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final UserRepositoryPort userRepository;
    private final SessionService sessionService;
    private final TwoFactorChallenges challenges;
    private final SessionCookies cookies;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        // The HTTP session only existed to carry OAuth state; it is not a login session.
        HttpSession oauthState = request.getSession(false);
        if (oauthState != null) {
            oauthState.invalidate();
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String provider =
                authentication instanceof OAuth2AuthenticationToken t
                        ? t.getAuthorizedClientRegistrationId()
                        : "unknown";
        String email = oauth2User.getAttribute("email");

        if (email == null || email.isBlank() || !emailVerified(provider, oauth2User)) {
            log.warn("OAuth sign-in refused: {} did not provide a verified email", provider);
            fail(response);
            return;
        }
        String normalized = email.trim().toLowerCase();

        User user =
                userRepository
                        .findByEmail(normalized)
                        .orElseGet(() -> userRepository.save(newStudent(normalized, oauth2User)));

        if (!user.isEnabled()) {
            fail(response);
            return;
        }

        if (user.isTwoFaEnabled()) {
            cookies.setChallenge(response, challenges.issue(user));
            response.sendRedirect(frontendUrl + "/#/login?twofa=required");
            return;
        }

        SessionService.Issued issued =
                sessionService.create(
                        user.getId(),
                        request.getRemoteAddr(),
                        request.getHeader(HttpHeaders.USER_AGENT));
        cookies.setSession(response, issued.token(), issued.maxAge());
        response.sendRedirect(frontendUrl + "/#/dashboard");
    }

    /**
     * Google says explicitly; Facebook only returns addresses its users have confirmed. Unknown
     * providers are not trusted.
     */
    private static boolean emailVerified(String provider, OAuth2User user) {
        if ("google".equals(provider)) {
            Object verified = user.getAttribute("email_verified");
            return Boolean.TRUE.equals(verified) || "true".equals(String.valueOf(verified));
        }
        return "facebook".equals(provider);
    }

    private static User newStudent(String email, OAuth2User oauth2User) {
        User u = new User();
        u.setEmail(email);
        u.setEnabled(true);
        u.setFirstName(
                firstNonBlank(
                        oauth2User.<String>getAttribute("given_name"),
                        oauth2User.<String>getAttribute("first_name")));
        u.setLastName(
                firstNonBlank(
                        oauth2User.<String>getAttribute("family_name"),
                        oauth2User.<String>getAttribute("last_name")));
        u.setRoles(Set.of(Role.STUDENT));
        // Not a BCrypt hash, so no password can ever match it: this account signs in via OAuth.
        u.setPasswordHash("OAUTH2_USER");
        return u;
    }

    private void fail(HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendUrl + "/#/login?oauth=failed");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
