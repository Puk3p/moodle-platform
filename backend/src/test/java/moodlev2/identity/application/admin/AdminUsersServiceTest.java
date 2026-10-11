package moodlev2.identity.application.admin;

import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserDeletedEvent;
import moodlev2.identity.api.UserEmailChangedEvent;
import moodlev2.identity.application.auth.SessionService;
import moodlev2.identity.infrastructure.persistence.ClassRepository;
import moodlev2.identity.infrastructure.persistence.SpringDataUserRepository;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;
import moodlev2.identity.web.admin.dto.UpdateStudentRequest;
import moodlev2.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminUsersServiceTest {

    private static final String ACTOR = "admin@test.com";

    @Mock private SpringDataUserRepository userRepository;
    @Mock private ClassRepository classRepository;
    @Mock private ApplicationEventPublisher events;
    @Mock private SessionService sessionService;

    @InjectMocks private AdminUsersService service;

    private final UserEntity admin = user(1, ACTOR, Role.ADMIN);
    private final UserEntity otherAdmin = user(2, "admin2@test.com", Role.ADMIN, Role.TEACHER);
    private final UserEntity student = user(3, "student@test.com", Role.STUDENT);
    private final UserEntity classmate = user(4, "taken@test.com", Role.STUDENT);

    @BeforeEach
    void setUp() {
        for (UserEntity u : new UserEntity[] {admin, otherAdmin, student, classmate}) {
            lenient().when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient().when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
    }

    private static UpdateStudentRequest edit(String email) {
        return new UpdateStudentRequest(" Ana ", " Pop ", email, null);
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    // ── Refusals ─────────────────────────────────────────────────────────────

    @Test
    void adminCannotEditDisableOrDeleteThemselves() {
        assertThatThrownBy(() -> service.updateStudent(1L, edit(ACTOR), ACTOR))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.disableTwoFactor(1L, ACTOR))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.deleteUser(1L, "ADMIN@test.com"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).delete(any());
        verifyNoInteractions(sessionService, events);
    }

    @Test
    void anotherAdministratorIsOffLimits() {
        assertThatThrownBy(() -> service.updateStudent(2L, edit("new@test.com"), ACTOR))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.disableTwoFactor(2L, ACTOR))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.deleteUser(2L, ACTOR))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(otherAdmin.getEmail()).isEqualTo("admin2@test.com");
        verify(userRepository, never()).delete(any());
        verifyNoInteractions(sessionService, events);
    }

    @Test
    void unknownUserIsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(99L, ACTOR))
                .isInstanceOf(NotFoundException.class);
    }

    // ── Editing ──────────────────────────────────────────────────────────────

    @Test
    void emailIsTrimmedAndLowerCased() {
        service.updateStudent(3L, edit("  New@Test.COM "), ACTOR);

        assertThat(student.getEmail()).isEqualTo("new@test.com");
        assertThat(student.getFirstName()).isEqualTo("Ana");
        assertThat(student.getLastName()).isEqualTo("Pop");
    }

    @Test
    void emailOwnedBySomeoneElseIsAConflict() {
        assertThatThrownBy(() -> service.updateStudent(3L, edit("Taken@test.com"), ACTOR))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.CONFLICT));

        assertThat(student.getEmail()).isEqualTo("student@test.com");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(sessionService, events);
    }

    @Test
    void changingTheEmailMovesChatHistoryAndSignsTheUserOut() {
        service.updateStudent(3L, edit("new@test.com"), ACTOR);

        verify(events)
                .publishEvent(new UserEmailChangedEvent(3L, "student@test.com", "new@test.com"));
        verify(sessionService).revokeAll(3L);
    }

    @Test
    void keepingTheEmailTouchesNeitherChatNorSessions() {
        service.updateStudent(3L, edit("student@test.com"), ACTOR);

        verify(userRepository).save(student);
        verifyNoInteractions(sessionService, events);
    }

    // ── 2FA reset and deletion ───────────────────────────────────────────────

    @Test
    void disablingTwoFactorSignsTheUserOutEverywhere() {
        student.setTwoFaEnabled(true);
        student.setTwoFaSecret("SECRET");

        service.disableTwoFactor(3L, ACTOR);

        assertThat(student.isTwoFaEnabled()).isFalse();
        assertThat(student.getTwoFaSecret()).isNull();
        verify(sessionService).revokeAll(3L);
    }

    @Test
    void deletingAUserRemovesTheirChatHistoryAndSessions() {
        service.deleteUser(3L, ACTOR);

        InOrder order = inOrder(events, sessionService, userRepository);
        order.verify(events).publishEvent(new UserDeletedEvent(3L, "student@test.com"));
        order.verify(sessionService).revokeAll(3L);
        order.verify(userRepository).delete(student);
    }

    @Test
    void teachersCanStillBeManaged() {
        UserEntity teacher = user(5, "teacher@test.com", Role.TEACHER);
        when(userRepository.findById(5L)).thenReturn(Optional.of(teacher));

        service.deleteUser(5L, ACTOR);

        verify(userRepository).delete(teacher);
        verify(events).publishEvent(new UserDeletedEvent(5L, "teacher@test.com"));
        verify(events, never()).publishEvent(any(UserEmailChangedEvent.class));
        verify(sessionService).revokeAll(5L);
    }
}
