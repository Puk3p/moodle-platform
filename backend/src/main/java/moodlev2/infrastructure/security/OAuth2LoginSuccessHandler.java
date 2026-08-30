package moodlev2.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import moodlev2.common.util.TokenHashUtil;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import moodlev2.domain.user.ports.UserRepositoryPort;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final TokenServicePort tokenService;
    private final UserRepositoryPort userRepository;
    private final SpringDataUserRepository jpaUserRepository;
    private final UserSessionRepository userSessionRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        String email = oauth2User.getAttribute("email");

        String firstName =
                fistNonNull(
                        oauth2User.<String>getAttribute("given_name"),
                        oauth2User.<String>getAttribute("first_name"));

        String lastName =
                fistNonNull(
                        oauth2User.<String>getAttribute("family_name"),
                        oauth2User.<String>getAttribute("last_name"));

        var user =
                userRepository
                        .findByEmail(email)
                        .orElseGet(
                                () -> {
                                    var u = new User();
                                    u.setEmail(email);
                                    u.setEnabled(true);
                                    u.setFirstName(firstName);
                                    u.setLastName(lastName);
                                    u.setRoles(Set.of(Role.STUDENT));

                                    u.setPasswordHash("OAUTH2_USER");

                                    return userRepository.save(u);
                                });

        String token = tokenService.generateToken(user, Duration.ofHours(1), Set.of("access:api"));

        // Save a session so the JWT filter accepts this token.
        var userEntity = jpaUserRepository.findByEmail(email).orElse(null);
        if (userEntity != null) {
            UserSessionEntity session = new UserSessionEntity();
            session.setUser(userEntity);
            session.setIpAddress(request.getRemoteAddr());
            session.setDeviceName("OAuth2 Login");
            session.setTokenSignature(TokenHashUtil.sha256(token));
            userSessionRepository.save(session);
        }

        response.sendRedirect(frontendUrl + "/login#token=" + token);
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
