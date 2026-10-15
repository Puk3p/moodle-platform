package moodlev2.assessment.infrastructure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Only the gateway (user tokens) and other services (service tokens) call this service, with RS256
 * tokens signed by identity-service. No sessions, no CSRF (the gateway enforces it).
 *
 * <p>The URL rules are core's, unchanged: authoring quizzes and the question bank is for staff;
 * students may start, submit and report proctoring on their own attempts. Authorities combine the
 * token scope ({@code SCOPE_user}/{@code SCOPE_internal}) with {@code ROLE_*} from its roles claim;
 * the caller's name is the {@code email} claim.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                                        .permitAll()
                                        .requestMatchers("/internal/**")
                                        .hasAuthority("SCOPE_internal")
                                        .requestMatchers("/api/question-bank/**", "/api/teacher/**")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/api/quizzes/create")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/*")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/*")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/**")
                                        .hasAuthority("SCOPE_user")
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter())));
        return http.build();
    }

    /** Quiz access passwords are stored hashed, as in core (same BCrypt format). */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static JwtAuthenticationConverter converter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("email");
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    if (roles != null) {
                        roles.forEach(
                                r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                    }
                    return authorities;
                });
        return converter;
    }
}
