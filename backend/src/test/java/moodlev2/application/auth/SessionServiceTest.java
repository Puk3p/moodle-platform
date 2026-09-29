package moodlev2.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moodlev2.common.util.TokenHashUtil;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.UserSessionRepository;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserSessionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-29T10:00:00Z");
    private static final String CHROME = "Mozilla/5.0 (Macintosh) Chrome/140";
    private static final String OTHER = "curl/8.0";

    @Mock private UserSessionRepository sessions;
    @Mock private SpringDataUserRepository users;
    @Mock private ApplicationEventPublisher events;

    private SessionService service;
    private final UserEntity alex = new UserEntity();
    private final List<UserSessionEntity> stored = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new SessionService(sessions, users, events, Clock.fixed(NOW, ZoneOffset.UTC));
        alex.setId(1L);
        alex.setEmail("student@test.com");
        alex.setActive(true);
        lenient().when(users.findById(1L)).thenReturn(Optional.of(alex));
        lenient()
                .when(sessions.save(any(UserSessionEntity.class)))
                .thenAnswer(
                        inv -> {
                            UserSessionEntity s = inv.getArgument(0);
                            if (!stored.contains(s)) {
                                stored.add(s);
                            }
                            return s;
                        });
        lenient()
                .when(sessions.findAllByUserIdOrderByCreatedAtAsc(anyLong()))
                .thenAnswer(inv -> List.copyOf(stored));
    }

    private UserSessionEntity session(
            String token, String ua, Instant created, Instant lastActive) {
        UserSessionEntity s = new UserSessionEntity();
        s.setId(7L);
        s.setUser(alex);
        s.setTokenSignature(TokenHashUtil.sha256(token));
        s.setUserAgentHash(SessionService.hashUserAgent(ua));
        s.setCreatedAt(created);
        s.setLastActive(lastActive);
        s.setExpiresAt(created.plus(SessionService.ABSOLUTE_LIFETIME));
        when(sessions.findWithUserByTokenSignature(TokenHashUtil.sha256(token)))
                .thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void theDatabaseNeverHoldsTheRawToken() {
        SessionService.Issued issued = service.create(1L, "10.0.0.1", CHROME);

        UserSessionEntity row = stored.get(0);
        assertThat(issued.token()).hasSizeGreaterThanOrEqualTo(43); // 256 bits, base64url
        assertThat(row.getTokenSignature())
                .isEqualTo(TokenHashUtil.sha256(issued.token()))
                .isNotEqualTo(issued.token());
        assertThat(row.getExpiresAt()).isEqualTo(NOW.plus(SessionService.ABSOLUTE_LIFETIME));
        assertThat(row.getDeviceName()).isEqualTo("macOS");
    }

    @Test
    void aValidCookieFromTheSameBrowserResolves() {
        session("tok", CHROME, NOW.minus(Duration.ofHours(1)), NOW.minus(Duration.ofMinutes(5)));

        assertThat(service.resolve("tok", CHROME, "10.0.0.1"))
                .hasValueSatisfying(r -> assertThat(r.user()).isSameAs(alex));
    }

    @Test
    void aCookieReplayedFromAnotherBrowserIsRejectedAndTheSessionKilled() {
        UserSessionEntity s = session("tok", CHROME, NOW.minus(Duration.ofHours(1)), NOW);

        assertThat(service.resolve("tok", OTHER, "203.0.113.9")).isEmpty();
        verify(sessions).deleteAll(List.of(s));
        ArgumentCaptor<SessionService.SessionsRevokedEvent> event =
                ArgumentCaptor.forClass(SessionService.SessionsRevokedEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().tokenHashes()).containsExactly(s.getTokenSignature());
    }

    @Test
    void idleSessionsExpire() {
        UserSessionEntity s =
                session(
                        "tok",
                        CHROME,
                        NOW.minus(Duration.ofHours(3)),
                        NOW.minus(SessionService.IDLE_TIMEOUT).minusSeconds(1));

        assertThat(service.resolve("tok", CHROME, "10.0.0.1")).isEmpty();
        verify(sessions).deleteAll(List.of(s));
    }

    @Test
    void sessionsEndAtTheirAbsoluteLifetimeEvenWhenActive() {
        UserSessionEntity s =
                session(
                        "tok",
                        CHROME,
                        NOW.minus(SessionService.ABSOLUTE_LIFETIME),
                        NOW.minusSeconds(10));

        assertThat(service.resolve("tok", CHROME, "10.0.0.1")).isEmpty();
        verify(sessions).deleteAll(List.of(s));
    }

    @Test
    void deactivatingAnAccountEndsItsSessionsOnTheNextRequest() {
        UserSessionEntity s = session("tok", CHROME, NOW.minus(Duration.ofHours(1)), NOW);
        alex.setActive(false);

        assertThat(service.resolve("tok", CHROME, "10.0.0.1")).isEmpty();
        verify(sessions).deleteAll(List.of(s));
    }

    @Test
    void unknownOrGarbageTokensResolveToNothing() {
        assertThat(service.resolve("nope", CHROME, "10.0.0.1")).isEmpty();
        assertThat(service.resolve("", CHROME, "10.0.0.1")).isEmpty();
        assertThat(service.resolve("x".repeat(500), CHROME, "10.0.0.1")).isEmpty();
        verify(sessions, never()).deleteAll(any());
    }

    @Test
    void theOldestSessionsAreEvictedBeyondTheCap() {
        for (int i = 0; i < SessionService.MAX_SESSIONS_PER_USER + 2; i++) {
            service.create(1L, "10.0.0.1", CHROME);
        }

        ArgumentCaptor<List<UserSessionEntity>> deleted = ArgumentCaptor.forClass(List.class);
        verify(sessions, org.mockito.Mockito.atLeastOnce()).deleteAll(deleted.capture());
        assertThat(deleted.getAllValues().stream().mapToInt(List::size).sum()).isGreaterThan(0);
    }

    @Test
    void signingOutOthersKeepsTheCurrentSession() {
        service.create(1L, "10.0.0.1", CHROME);
        service.create(1L, "10.0.0.2", CHROME);
        String keep = stored.get(1).getTokenSignature();

        service.revokeOthers(1L, keep);

        verify(sessions).deleteAll(List.of(stored.get(0)));
    }
}
