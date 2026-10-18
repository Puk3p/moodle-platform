package moodlev2.identity.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The RSA key pair that signs internal tokens. Other services only ever see the public half, via
 * the JWKS endpoint.
 *
 * <p>A pair configured in {@code security.internal-jwt.*} (PEM, PKCS#8 private / X.509 public) is
 * used as given. Otherwise the pair is generated on this service's very first start and kept in its
 * database (table {@code signing_keys}): every restart and every instance then signs with the same
 * key, so no token in flight stops being valid and services never have to re-fetch the JWKS. Its
 * key id is the key's thumbprint, so it stays the same too.
 */
@Component
public final class InternalJwtKeys {

    private static final Logger log = LoggerFactory.getLogger(InternalJwtKeys.class);

    /** The one row the stored pair lives in. */
    static final String STORED_KEY_ID = "current";

    private final RSAKey signingKey;

    @Autowired
    public InternalJwtKeys(
            @Value("${security.internal-jwt.private-key:}") String privatePem,
            @Value("${security.internal-jwt.public-key:}") String publicPem,
            JdbcTemplate jdbc) {
        this(configuredOrStored(privatePem, publicPem, jdbc));
    }

    private InternalJwtKeys(KeyPair pair) {
        try {
            this.signingKey =
                    new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                            .privateKey((RSAPrivateKey) pair.getPrivate())
                            .keyIDFromThumbprint()
                            .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not build the internal signing key", e);
        }
    }

    /** Private: only this application signs. */
    public RSAKey signingKey() {
        return signingKey;
    }

    public RSAPublicKey publicKey() {
        try {
            return signingKey.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The public half only, as published on the JWKS endpoint. */
    public JWKSet publicJwks() {
        return new JWKSet(signingKey.toPublicJWK());
    }

    private static KeyPair configuredOrStored(
            String privatePem, String publicPem, JdbcTemplate jdbc) {
        boolean hasPrivate = !privatePem.isBlank();
        boolean hasPublic = !publicPem.isBlank();
        if (hasPrivate && hasPublic) {
            log.info("Signing internal tokens with the configured key pair");
            return new KeyPair(publicKey(publicPem), privateKey(privatePem));
        }
        if (hasPrivate || hasPublic) {
            throw new IllegalStateException(
                    "security.internal-jwt: configure both private-key and public-key, or neither");
        }
        return stored(jdbc);
    }

    /**
     * The pair kept in the database, created if there is none yet. Instances starting together may
     * both try to store theirs; the first insert wins and every instance reads that one back.
     */
    private static KeyPair stored(JdbcTemplate jdbc) {
        KeyPair fresh = generate();
        int created =
                jdbc.update(
                        "INSERT IGNORE INTO signing_keys (id, private_key, public_key)"
                                + " VALUES (?, ?, ?)",
                        STORED_KEY_ID,
                        pem("PRIVATE KEY", fresh.getPrivate()),
                        pem("PUBLIC KEY", fresh.getPublic()));
        log.info(
                created == 1
                        ? "Generated the internal token signing key and stored it in the database"
                        : "Signing internal tokens with the key stored in the database");
        return jdbc.queryForObject(
                "SELECT private_key, public_key FROM signing_keys WHERE id = ?",
                (rs, row) ->
                        new KeyPair(
                                publicKey(rs.getString("public_key")),
                                privateKey(rs.getString("private_key"))),
                STORED_KEY_ID);
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** PKCS#8 for a private key, X.509 for a public one: what {@link #der} reads back. */
    static String pem(String type, Key key) {
        return "-----BEGIN "
                + type
                + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(key.getEncoded())
                + "\n-----END "
                + type
                + "-----\n";
    }

    private static RSAPrivateKey privateKey(String pem) {
        try {
            return (RSAPrivateKey)
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(new PKCS8EncodedKeySpec(der(pem)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "security.internal-jwt.private-key is not a PKCS#8 RSA key", e);
        }
    }

    private static RSAPublicKey publicKey(String pem) {
        try {
            return (RSAPublicKey)
                    KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der(pem)));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "security.internal-jwt.public-key is not an X.509 RSA key", e);
        }
    }

    private static byte[] der(String pem) {
        String base64 = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
