package moodlev2.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Spring Security is on the classpath only for the OAuth2 client (the gateway's own token for
 * identity-service). It must not authenticate browsers itself: identity-service authenticates its
 * own routes (it issues the session cookie), and the {@code SessionToken} and {@code
 * DoubleSubmitCsrf} route filters guard every other service.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
        return http.authorizeExchange(e -> e.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .headers(ServerHttpSecurity.HeaderSpec::disable)
                .build();
    }
}
