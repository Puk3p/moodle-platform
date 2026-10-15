package moodlev2.platform.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Only the gateway (user tokens, swapped for the browser's session cookie) and other services
 * (service tokens) call this application, with RS256 tokens signed by identity-service. No
 * sessions, no cookies, no CSRF: the gateway checks CSRF before it forwards anything, and CORS is
 * its job too.
 *
 * <p>Authorities combine the token scope ({@code SCOPE_user}/{@code SCOPE_internal}) with {@code
 * ROLE_*} from its roles claim; the caller's name is the {@code email} claim.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
                                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
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
                                // A denial is answered with sendError(), which the container
                                // re-dispatches to /error with an empty SecurityContext; without
                                // this a genuine 403 would be reported as a misleading 401.
                                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers("/actuator/health/**", "/actuator/info")
                                        .permitAll()
                                        .requestMatchers("/internal/**")
                                        .hasAuthority("SCOPE_internal")
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
                                                "/api/resources/options",
                                                "/api/courses/*/preview",
                                                "/api/courses/*/resources")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers("/api/teacher/**")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/courses/create")
                                        .hasAnyRole("TEACHER", "ADMIN")
                                        .requestMatchers("/api/**")
                                        .hasAuthority("SCOPE_user")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter())));
        return http.build();
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
