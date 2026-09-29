package moodlev2.application.auth.implementations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-29T10:00:15Z");
    private static final String SECRET = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

    @Mock private SpringDataUserRepository users;
    private TwoFactorService service;
    private final UserEntity alex = new UserEntity();

    @BeforeEach
    void setUp() {
        service = new TwoFactorService(users, Clock.fixed(NOW, ZoneOffset.UTC));
        alex.setId(1L);
        alex.setEmail("student@test.com");
        alex.setTwoFaSecret(SECRET);
        alex.setTwoFaEnabled(true);
        lenient().when(users.findByEmail(alex.getEmail())).thenReturn(Optional.of(alex));
    }

    private static String codeAt(long step) throws Exception {
        return new DefaultCodeGenerator(HashingAlgorithm.SHA1).generate(SECRET, step);
    }

    private static long stepNow() {
        return NOW.getEpochSecond() / 30;
    }

    @Test
    void theCurrentCodeIsAccepted() throws Exception {
        assertThat(service.verifyCode(alex.getEmail(), codeAt(stepNow()))).isTrue();
    }

    @Test
    void aCodeCannotBeReplayed() throws Exception {
        String code = codeAt(stepNow());
        assertThat(service.verifyCode(alex.getEmail(), code)).isTrue();

        assertThat(service.verifyCode(alex.getEmail(), code)).isFalse();
    }

    @Test
    void anOlderCodeIsRefusedOnceANewerOneWasUsed() throws Exception {
        assertThat(service.verifyCode(alex.getEmail(), codeAt(stepNow()))).isTrue();

        assertThat(service.verifyCode(alex.getEmail(), codeAt(stepNow() - 1))).isFalse();
    }

    @Test
    void codesOutsideTheDriftWindowAreRefused() throws Exception {
        assertThat(service.verifyCode(alex.getEmail(), codeAt(stepNow() - 3))).isFalse();
        assertThat(service.verifyCode(alex.getEmail(), "12345")).isFalse();
        assertThat(service.verifyCode(alex.getEmail(), "abcdef")).isFalse();
        assertThat(service.verifyCode(alex.getEmail(), null)).isFalse();
    }

    @Test
    void setupNeverRevealsTheSecretOnceEnabled() {
        assertThatThrownBy(() -> service.setupTwoFactor(alex.getEmail()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        t ->
                                assertThat(((ResponseStatusException) t).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }
}
