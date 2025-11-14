package moodlev2.infrastructure.security;

import io.jsonwebtoken.*;
import moodlev2.domain.auth.ports.TokenServicePort;
import moodlev2.domain.user.Role;
import moodlev2.domain.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


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
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
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

    @Override
    public TokenPayload parse(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token);

            Claims claims = jws.getBody();

            Long userId = claims.get("uid", Long.class);
            if (!(userId instanceof Long)) {
                throw new JwtException("Invalid user ID in token");
            }

            String email = claims.get("email", String.class);

            List<String> roleNames = claims.get("roles", List.class);

            Set<Role> roles = roleNames == null
                    ? Set.of()
                    : roleNames.stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());

            Instant expiresAt = claims.getExpiration().toInstant();

            return new TokenPayload(userId, email, roles, expiresAt);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token", e);
        }
    }

    @Override
    public boolean isValid(String token) {
        try {
            TokenPayload payload = parse(token);
            return payload.expiresAt().isAfter(Instant.now());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}