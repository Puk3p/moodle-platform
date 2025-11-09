package moodlev2.infrastructure.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Value;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.User;
import org.springframework.security.core.token.TokenService;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;


@Component
public class JwtServiceAdapter implements TokenServicePort {
    private final Key signingKey;
    private final String issuer;

    //link util, de aici am luat
    //https://medium.com/@th.chousiadas/spring-security-architecture-of-jwt-authentication-a7967a8ee309
    public JwtServiceAdapter(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer:moodlev2}") String issuer
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public String generateToken(User user, Duration validity, Set<String> scopes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validity);

        List<String> roleName = user.getRoles().stream()
                .map(Enum::name)
                .toList();

        String scopesString;
        if (scopes == null || scopes.isEmpty()) {
            return null;
        } else {
            scopesString = String.join(" ", scopes);
        }

        JwtBuilder builder = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .setIssuer(issuer)
                .setIssuedAt(java.util.Date.from(now))
                .setExpiration(java.util.Date.from(expiresAt))
                .claim("uid", user.getId())
                .claim("email", user.getEmail())
                .claim("roles", roleName);

        if (scopesString != null) {
            builder.claim("scopes", scopesString);
        }

        return builder
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}