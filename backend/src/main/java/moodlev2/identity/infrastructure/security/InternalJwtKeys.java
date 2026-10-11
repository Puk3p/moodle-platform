package moodlev2.identity.infrastructure.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The RSA key pair that signs internal tokens. Other services only ever see the public half, via
 * the JWKS endpoint.
 *
 * <p>Configure both halves (PEM, PKCS#8 private / X.509 public) in production, so every instance
 * signs with the same key and a restart does not invalidate tokens in flight. Without them a pair
 * is generated at start-up: fine for one instance, since the tokens live for seconds and consumers
 * re-fetch the JWKS when they meet an unknown key id.
 */
@Component
public final class InternalJwtKeys {

    private static final Logger log = LoggerFactory.getLogger(InternalJwtKeys.class);

    private final RSAKey signingKey;

    public InternalJwtKeys(
            @Value("${security.internal-jwt.private-key:}") String privatePem,
            @Value("${security.internal-jwt.public-key:}") String publicPem) {
        KeyPair pair;
        if (privatePem.isBlank() || publicPem.isBlank()) {
            log.warn(
                    "security.internal-jwt keys not configured; generated a key pair for this run."
                            + " Configure them when running more than one instance.");
            pair = generate();
        } else {
            pair = new KeyPair(publicKey(publicPem), privateKey(privatePem));
        }
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

    private static KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
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
