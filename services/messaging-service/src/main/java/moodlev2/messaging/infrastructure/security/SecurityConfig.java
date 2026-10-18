package moodlev2.messaging.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

/**
 * Only the gateway talks to this service. It authenticates the browser (session cookie, CSRF) and
 * forwards a short-lived user token signed by identity-service; this service checks that token and
 * nothing else, so it keeps no session and needs no CSRF protection of its own.
 *
 * <p>The caller is identified by the token's {@code email} claim, which is how chat addresses
 * people.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter principal = new JwtAuthenticationConverter();
        principal.setPrincipalClaimName("email");

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                                        .permitAll()
                                        // The upgrade is authenticated in the handshake itself.
                                        .requestMatchers("/ws/**")
                                        .permitAll()
                                        .requestMatchers("/api/chat/**")
                                        .hasAuthority("SCOPE_user")
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(principal)));
        return http.build();
    }
}
