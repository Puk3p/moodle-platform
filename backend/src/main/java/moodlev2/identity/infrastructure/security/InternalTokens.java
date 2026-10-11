package moodlev2.identity.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import moodlev2.identity.api.UserSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Issues the RS256 tokens used between the gateway and the services. They never reach a browser:
 * the browser keeps its HttpOnly session cookie, and the gateway swaps it for a {@link #forUser
 * user token} on every request.
 *
 * <ul>
 *   <li>user tokens: {@code sub} = user id, scope {@code user}, plus email, roles, class and the
 *       session ({@code sid}, the token hash) so sockets can be closed when it is revoked;
 *   <li>service tokens: {@code sub} = {@code service:<client>}, scope {@code internal}, for calls
 *       that act on no user's behalf (the client-credentials grant).
 * </ul>
 */
@Component
public class InternalTokens {

    public static final String AUDIENCE = "moodle-internal";
    public static final Duration USER_TOKEN_LIFETIME = Duration.ofSeconds(60);
    public static final Duration SERVICE_TOKEN_LIFETIME = Duration.ofMinutes(5);

    private final JwtEncoder encoder;
    private final String issuer;
    private final Clock clock;

    @Autowired
    public InternalTokens(
            InternalJwtKeys keys,
            @Value("${security.internal-jwt.issuer:moodle-core}") String issuer) {
        this(keys, issuer, Clock.systemUTC());
    }

    InternalTokens(InternalJwtKeys keys, String issuer, Clock clock) {
        this.encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(keys.signingKey())));
        this.issuer = issuer;
        this.clock = clock;
    }

    public String forUser(UserSummary user, String sessionHash) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder claims =
                base(now, USER_TOKEN_LIFETIME)
                        .subject(String.valueOf(user.id()))
                        .claim("scope", "user")
                        .claim("email", user.email())
                        .claim("roles", user.roles().stream().map(Enum::name).sorted().toList())
                        .claim("sid", sessionHash);
        if (user.classId() != null) {
            claims.claim("classId", user.classId());
        }
        return sign(claims.build());
    }

    public String forService(String clientId) {
        Instant now = clock.instant();
        return sign(
                base(now, SERVICE_TOKEN_LIFETIME)
                        .subject("service:" + clientId)
                        .claim("scope", "internal")
                        .build());
    }

    private JwtClaimsSet.Builder base(Instant now, Duration lifetime) {
        return JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(AUDIENCE))
                .issuedAt(now)
                .expiresAt(now.plus(lifetime));
    }

    private String sign(JwtClaimsSet claims) {
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
