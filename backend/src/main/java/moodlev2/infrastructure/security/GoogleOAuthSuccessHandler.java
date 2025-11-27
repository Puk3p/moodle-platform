package moodlev2.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.PasswordHasherPort;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenServicePort tokenService;
    private final UserRepositoryPort userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {


        var oauth2User = (org.springframework.security.oauth2.core.user.OAuth2User)
                authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");

        var user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    var u = new User();
                    u.setEmail(email);
                    u.setEnabled(true);
                    u.setFirstName(oauth2User.getAttribute("given_name"));
                    u.setLastName(oauth2User.getAttribute("family_name"));
                    u.setRoles(Set.of(Role.STUDENT));

                    u.setPasswordHash("GOOGLE_OAUTH_USER");

                    return userRepository.save(u);
                });

        String token = tokenService.generateToken(
                user,
                Duration.ofHours(1),
                Set.of("access:api")
        );

        response.sendRedirect("http://localhost:4200/login?token=" + token);
    }
}
