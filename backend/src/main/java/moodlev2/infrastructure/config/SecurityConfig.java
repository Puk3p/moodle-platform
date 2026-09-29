package moodlev2.infrastructure.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import moodlev2.infrastructure.security.OAuth2LoginSuccessHandler;
import moodlev2.infrastructure.security.RateLimitFilter;
import moodlev2.infrastructure.security.SessionAuthenticationFilter;
import moodlev2.infrastructure.security.SessionCookies;
import moodlev2.infrastructure.security.SpaCsrfTokenRequestHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oauthSuccessHandler;
    private final SessionCookies cookies;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                // Authentication is a cookie, so every state-changing request must also prove it
                // came from our own page: the SPA echoes the readable XSRF cookie in a header,
                // which
                // a cross-site page cannot do. SameSite=Strict already stops the browser attaching
                // the session cookie cross-site; this is the second, independent layer.
                .csrf(
                        csrf ->
                                csrf.csrfTokenRepository(csrfTokenRepository())
                                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                                        // The OAuth callback is the provider's cross-site GET
                                        // redirect (protected by the OAuth state parameter); /ws is
                                        // a GET upgrade whose frames are receive-only.
                                        .ignoringRequestMatchers(
                                                "/login/oauth2/**", "/oauth2/**", "/ws/**"))
                .headers(
                        headers ->
                                headers.contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                // The API never serves a page
                                                                // that should render or run
                                                                // anything; a sandbox makes even
                                                                // a mislabeled upload inert.
                                                                "default-src 'none'; frame-ancestors"
                                                                        + " 'none'; base-uri 'none';"
                                                                        + " form-action 'none';"
                                                                        + " sandbox"))
                                        .frameOptions(
                                                org.springframework.security.config.annotation.web
                                                                .configurers.HeadersConfigurer
                                                                .FrameOptionsConfig
                                                        ::deny)
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31_536_000))
                                        .referrerPolicy(
                                                r ->
                                                        r.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER))
                                        .addHeaderWriter(
                                                new StaticHeadersWriter(
                                                        "Permissions-Policy",
                                                        "camera=(), microphone=(), geolocation=(),"
                                                                + " payment=(), usb=()"))
                                        .addHeaderWriter(
                                                new StaticHeadersWriter(
                                                        "Cross-Origin-Resource-Policy",
                                                        "same-site")))
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
                                // Spring Security answers a denial with sendError(), which the
                                // container re-dispatches to /error. That ERROR dispatch carries
                                // an empty SecurityContext, so without this it is matched by
                                // anyRequest().authenticated() and a genuine 403 is reported to
                                // the client as a misleading 401.
                                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        // Under /api/auth but only meaningful when signed in.
                                        .requestMatchers(
                                                "/api/auth/session",
                                                "/api/auth/logout",
                                                "/api/auth/2fa/**")
                                        .authenticated()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        .requestMatchers("/oauth2/**")
                                        .permitAll()
                                        .requestMatchers("/login/oauth2/**")
                                        .permitAll()
                                        // Authenticated in the handshake itself (cookie + origin).
                                        .requestMatchers("/ws/**")
                                        .permitAll()
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        // API docs (disabled by default, see springdoc.*) are for
                                        // administrators only, never any signed-in student.
                                        .requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .hasRole("ADMIN")
                                        // Lists and views only teacher screens use. Students get
                                        // their own filtered views elsewhere.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/api/courses/list",
                                                "/api/classes/list",
                                                "/api/resources/options",
                                                "/api/courses/*/preview",
                                                "/api/courses/*/resources")
                                        .hasAnyRole("TEACHER", "ADMIN")
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
                                        // Quiz authoring is staff-only. Scoped by HTTP method so
                                        // the student flows on the same prefix
                                        // (POST /{id}/start, POST /submit) stay reachable. This
                                        // duplicates the @PreAuthorize on QuizController on
                                        // purpose: without it these fall through to the catch-all
                                        // `authenticated()` and any logged-in student can delete
                                        // or rewrite any quiz.
                                        .requestMatchers(HttpMethod.POST, "/api/quizzes/create")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers(HttpMethod.PUT, "/api/quizzes/*")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers(HttpMethod.DELETE, "/api/quizzes/*")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .oauth2Login(
                        oauth ->
                                oauth.successHandler(oauthSuccessHandler)
                                        .failureHandler(
                                                (request, response, exception) ->
                                                        response.sendRedirect(
                                                                frontendUrl
                                                                        + "/#/login?oauth=failed")))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Readable by the SPA (it must echo the value), Secure, SameSite=Strict, and {@code __Host-}
     * prefixed in production so a sibling subdomain cannot plant a known token (cookie tossing).
     */
    private CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(cookies.isSecure() ? "__Host-XSRF-TOKEN" : "XSRF-TOKEN");
        repository.setCookieCustomizer(
                c -> c.secure(cookies.isSecure()).sameSite("Strict").path("/"));
        return repository;
    }

    /**
     * The session filter is a bean (it needs injected services) and would otherwise also be
     * registered as a plain servlet filter; it must only run inside the security chain.
     */
    @Bean
    public FilterRegistrationBean<SessionAuthenticationFilter> sessionFilterRegistration(
            SessionAuthenticationFilter filter) {
        FilterRegistrationBean<SessionAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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
