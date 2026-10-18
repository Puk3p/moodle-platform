package moodlev2.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.Test;

/** Configured keys; the stored ones are covered against a real database in InternalApiIT. */
class InternalJwtKeysTest {

    private static KeyPair pair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    @Test
    void aConfiguredPairIsUsedAsGivenWithoutTouchingTheDatabase() throws Exception {
        KeyPair pair = pair();

        InternalJwtKeys keys =
                new InternalJwtKeys(
                        InternalJwtKeys.pem("PRIVATE KEY", pair.getPrivate()),
                        InternalJwtKeys.pem("PUBLIC KEY", pair.getPublic()),
                        null);

        assertThat(keys.publicKey()).isEqualTo(pair.getPublic());
        assertThat(keys.publicJwks().getKeys().get(0).getKeyID())
                .isEqualTo(
                        new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                                .keyIDFromThumbprint()
                                .build()
                                .getKeyID());
        assertThat(keys.publicJwks().getKeys().get(0).isPrivate()).isFalse();
    }

    @Test
    void halfAConfiguredPairIsAMistakeNotAReasonToGenerateOne() throws Exception {
        String publicOnly = InternalJwtKeys.pem("PUBLIC KEY", pair().getPublic());

        assertThatThrownBy(() -> new InternalJwtKeys("", publicOnly, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("both");
    }
}
