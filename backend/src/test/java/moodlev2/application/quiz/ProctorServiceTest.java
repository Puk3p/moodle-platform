package moodlev2.application.quiz;

import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ProctorEventRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProctorEventEntity;
import moodlev2.infrastructure.persistence.jpa.entity.QuizAttemptEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProctorServiceTest {

    @Mock private ProctorEventRepository proctorEventRepository;
    @Mock private QuizAttemptRepository attemptRepository;

    @InjectMocks private ProctorService proctorService;

    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);

    private static final List<ProctorService.ProctorInput> EVENTS =
            List.of(new ProctorService.ProctorInput("TAB_HIDDEN", null, 1L));

    private QuizAttemptEntity attempt(String status) {
        QuizAttemptEntity a = new QuizAttemptEntity();
        a.setId(500L);
        a.setUser(student);
        a.setStatus(status);
        when(attemptRepository.findById(500L)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void eventsOnAnOpenAttemptAreStored() {
        attempt("IN_PROGRESS");

        proctorService.record(500L, student.getEmail(), EVENTS);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProctorEventEntity>> rows = ArgumentCaptor.forClass(List.class);
        verify(proctorEventRepository).saveAll(rows.capture());
        assertThat(rows.getValue())
                .extracting(ProctorEventEntity::getEventType)
                .containsExactly("TAB_HIDDEN");
    }

    @Test
    void eventsAfterSubmissionAreDroppedSoTheReportCannotChange() {
        attempt("COMPLETED");

        proctorService.record(500L, student.getEmail(), EVENTS);

        verify(proctorEventRepository, never()).saveAll(anyList());
        verify(proctorEventRepository, never()).save(any());
    }

    @Test
    void someoneElsesAttemptIsStillForbidden() {
        attempt("COMPLETED");

        assertThatThrownBy(() -> proctorService.record(500L, "other@test.com", EVENTS))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        t ->
                                assertThat(((ResponseStatusException) t).getStatusCode().value())
                                        .isEqualTo(HttpStatus.FORBIDDEN.value()));
    }
}
