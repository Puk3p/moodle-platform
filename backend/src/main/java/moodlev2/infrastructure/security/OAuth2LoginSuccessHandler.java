package moodlev2.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenServicePort tokenService;
    private final UserRepositoryPort userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {


        OAuth2User oauth2User = (OAuth2User)
                authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");

        String firstName = fistNonNull(
                oauth2User.<String>getAttribute("given_name"),
                oauth2User.<String>getAttribute("first_name")
        );

        String lastName = fistNonNull(
                oauth2User.<String>getAttribute("family_name"),
                oauth2User.<String>getAttribute("last_name")
        );

        var user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    var u = new User();
                    u.setEmail(email);
                    u.setEnabled(true);
                    u.setFirstName(firstName);
                    u.setLastName(lastName);
                    u.setRoles(Set.of(Role.STUDENT));

                    u.setPasswordHash("OAUTH2_USER");

                    return userRepository.save(u);
                });

        String token = tokenService.generateToken(
                user,
                Duration.ofHours(1),
                Set.of("access:api")
        );

        response.sendRedirect("http://localhost:4200/login?token=" + token);
    }

    private String fistNonNull(String... valori) {
        for (String valoare : valori) {
            if (valoare != null && !valoare.isBlank()) {
                return valoare;
            }
        }
        return "";
    }
}
