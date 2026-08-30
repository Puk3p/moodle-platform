package moodlev2.infrastructure.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.security.JwtAuthenticationFilter;
import moodlev2.infrastructure.security.OAuth2LoginSuccessHandler;
import moodlev2.infrastructure.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler googleOAuthSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self'; frame-ancestors"
                                                                        + " 'none'"))
                                        .frameOptions(
                                                org.springframework.security.config.annotation.web
                                                                .configurers.HeadersConfigurer
                                                                .FrameOptionsConfig
                                                        ::deny)
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31_536_000)))
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        response.sendError(
                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                "Unauthorized"))
                                        .accessDeniedHandler(
                                                (request, response, deniedException) ->
                                                        response.sendError(
                                                                HttpServletResponse.SC_FORBIDDEN,
                                                                "Forbidden")))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/auth/**")
                                        .permitAll()
                                        .requestMatchers("/oauth2/**")
                                        .permitAll()
                                        .requestMatchers("/login/oauth2/**")
                                        .permitAll()
                                        .requestMatchers("/ws/**")
                                        .permitAll()
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/teacher/**")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/question-bank/**")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/users/teachers")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/courses/create")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/courses/**")
                                        .authenticated()
                                        .requestMatchers("/api/calendar/**")
                                        .authenticated()
                                        .requestMatchers("/api/grades/**")
                                        .authenticated()
                                        .requestMatchers("/api/resources/**")
                                        .authenticated()
                                        .requestMatchers("/api/users/**")
                                        .authenticated()
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(oauth -> oauth.successHandler(googleOAuthSuccessHandler))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
