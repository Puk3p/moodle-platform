package moodlev2.application.quiz;

import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ProctorEventRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProctorEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.support.Fixtures.MutableClock;
import moodlev2.web.quiz.dto.ProctorReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProctorServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-11T10:00:00Z");

    @Mock private ProctorEventRepository proctorEventRepository;
    @Mock private QuizAttemptRepository attemptRepository;

    private final MutableClock clock = new MutableClock(NOW);
    private ProctorService proctorService;

    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);

    private static final List<ProctorService.ProctorInput> EVENTS =
            List.of(new ProctorService.ProctorInput("TAB_HIDDEN", null, 1L));

    @BeforeEach
    void setUp() {
        proctorService = new ProctorService(proctorEventRepository, attemptRepository, clock);
    }

    private QuizAttemptEntity attempt(String status) {
        QuizAttemptEntity a = new QuizAttemptEntity();
        a.setId(500L);
        a.setUser(student);
        a.setStatus(status);
        a.setStartedAt(NOW.minus(Duration.ofMinutes(30)));
        when(attemptRepository.findById(500L)).thenReturn(Optional.of(a));
        return a;
    }

    private static ProctorEventEntity event(String type, Instant at) {
        ProctorEventEntity e = new ProctorEventEntity();
        e.setEventType(type);
        e.setOccurredAt(at);
        return e;
    }

    @SuppressWarnings("unchecked")
    private List<ProctorEventEntity> savedRows() {
        ArgumentCaptor<List<ProctorEventEntity>> rows = ArgumentCaptor.forClass(List.class);
        verify(proctorEventRepository).saveAll(rows.capture());
        return rows.getValue();
    }

    // --- record -----------------------------------------------------------------------------

    @Test
    void eventsOnAnOpenAttemptAreStored() {
        attempt("IN_PROGRESS");

        proctorService.record(500L, student.getEmail(), EVENTS, null);

        assertThat(savedRows())
                .extracting(ProctorEventEntity::getEventType)
                .containsExactly("TAB_HIDDEN");
    }

    @Test
    void eventsAfterSubmissionAreDroppedSoTheReportCannotChange() {
        attempt("COMPLETED");

        proctorService.record(500L, student.getEmail(), EVENTS, null);

        verify(proctorEventRepository, never()).saveAll(anyList());
        verify(proctorEventRepository, never()).save(any());
    }

    @Test
    void someoneElsesAttemptIsStillForbidden() {
        attempt("COMPLETED");

        assertThatThrownBy(() -> proctorService.record(500L, "other@test.com", EVENTS, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        t ->
                                assertThat(((ResponseStatusException) t).getStatusCode().value())
                                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void eventTimesComeFromTheClientCorrectedForItsClockSkew() {
        attempt("IN_PROGRESS");
        // The student's laptop clock is an hour behind. They left five minutes ago and came back
        // ten seconds ago; both events arrive together, in one batch, now.
        long clientNow = NOW.minus(Duration.ofHours(1)).toEpochMilli();
        List<ProctorService.ProctorInput> batch =
                List.of(
                        new ProctorService.ProctorInput("TAB_HIDDEN", null, clientNow - 300_000),
                        new ProctorService.ProctorInput("TAB_VISIBLE", null, clientNow - 10_000));

        proctorService.record(500L, student.getEmail(), batch, clientNow);

        assertThat(savedRows())
                .extracting(ProctorEventEntity::getOccurredAt)
                .containsExactly(
                        NOW.minus(Duration.ofMinutes(5)), NOW.minus(Duration.ofSeconds(10)));
    }

    @Test
    void unusableClientTimesFallBackToArrivalOrAreClampedToTheAttempt() {
        QuizAttemptEntity attempt = attempt("IN_PROGRESS");
        long clientNow = NOW.toEpochMilli();
        List<ProctorService.ProctorInput> batch =
                List.of(
                        // claims to be from after the batch was sent
                        new ProctorService.ProctorInput("COPY", null, clientNow + 60_000),
                        // claims to be from before the attempt started
                        new ProctorService.ProctorInput("PASTE", null, clientNow - 3_600_000),
                        // no timestamp at all
                        new ProctorService.ProctorInput("TAB_HIDDEN", null, null));

        proctorService.record(500L, student.getEmail(), batch, clientNow);

        assertThat(savedRows())
                .extracting(ProctorEventEntity::getOccurredAt)
                .containsExactly(NOW, attempt.getStartedAt(), NOW);
    }

    @Test
    void anAttemptAtItsEventCeilingStopsAcceptingEvents() {
        attempt("IN_PROGRESS");
        when(proctorEventRepository.countByAttemptId(500L)).thenReturn(2_000L);

        proctorService.record(500L, student.getEmail(), EVENTS, null);

        verify(proctorEventRepository, never()).saveAll(anyList());
    }

    // --- report -----------------------------------------------------------------------------

    @Test
    void timeAwayPairsEachExitWithTheReturn() {
        attempt("IN_PROGRESS");
        when(proctorEventRepository.findByAttemptIdOrderByOccurredAtAscIdAsc(500L))
                .thenReturn(
                        List.of(
                                event("TAB_HIDDEN", NOW.minusSeconds(600)),
                                event("TAB_VISIBLE", NOW.minusSeconds(300)),
                                event("PASTE", NOW.minusSeconds(200)),
                                event("WINDOW_BLUR", NOW.minusSeconds(100)),
                                event("WINDOW_FOCUS", NOW.minusSeconds(40))));

        ProctorReportDto report = proctorService.report(500L);

        assertThat(report.leaveCount()).isEqualTo(2);
        assertThat(report.totalAwaySeconds()).isEqualTo(300 + 60);
        assertThat(report.pasteCount()).isEqualTo(1);
    }

    @Test
    void aStudentWhoNeverCameBackIsAwayUntilTheAttemptEnded() {
        QuizAttemptEntity attempt = attempt("COMPLETED");
        attempt.setCompletedAt(NOW.minusSeconds(60));
        when(proctorEventRepository.findByAttemptIdOrderByOccurredAtAscIdAsc(500L))
                .thenReturn(List.of(event("TAB_HIDDEN", NOW.minusSeconds(900))));

        ProctorReportDto report = proctorService.report(500L);

        assertThat(report.leaveCount()).isEqualTo(1);
        assertThat(report.totalAwaySeconds()).isEqualTo(840);
    }
}
