package moodlev2.identity.infrastructure.config;

import moodlev2.identity.infrastructure.security.InternalClients;
import moodlev2.identity.infrastructure.security.InternalJwtKeys;
import moodlev2.identity.infrastructure.security.InternalTokens;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security for {@code /internal/**}: the API the gateway and the other services call. Bearer tokens
 * only (this application's own RS256 tokens); no cookies, sessions or CSRF, since no browser ever
 * talks to it. The gateway does not route these paths at all.
 *
 * <p>Ordered before the browser-facing chain in {@link SecurityConfig}, which never sees them.
 */
@Configuration
@EnableConfigurationProperties(InternalClients.class)
public class InternalSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            InternalJwtKeys keys,
            @Value("${security.internal-jwt.issuer:moodle-identity}") String issuer)
            throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey()).build();
        OAuth2TokenValidator<Jwt> audience =
                new JwtClaimValidator<java.util.List<String>>(
                        "aud", aud -> aud != null && aud.contains(InternalTokens.AUDIENCE));
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(issuer), audience));

        http.securityMatcher("/internal/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/internal/oauth2/token",
                                                "/internal/.well-known/jwks.json")
                                        .permitAll()
                                        .anyRequest()
                                        .hasAuthority("SCOPE_internal"))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(decoder)));
        return http.build();
    }
}
