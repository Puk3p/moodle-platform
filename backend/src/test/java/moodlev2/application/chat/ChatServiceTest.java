package moodlev2.application.chat;

import static moodlev2.application.chat.ChatTestData.openAttempt;
import static moodlev2.application.chat.ChatTestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ChatMessageRepository;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ChatMessageEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.chat.dto.ChatMessageDto;
import moodlev2.web.chat.dto.ChatStatusDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository messageRepository;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SimpMessagingTemplate messaging;

    @InjectMocks private ChatService chatService;

    private final UserEntity teacher = user(1, "teacher@test.com", Role.TEACHER);
    private final UserEntity student = user(2, "student@test.com", Role.STUDENT);
    private final UserEntity classmate = user(3, "student4@test.com", Role.STUDENT);
    private final UserEntity admin = user(4, "admin@test.com", Role.ADMIN);

    @BeforeEach
    void setUp() {
        for (UserEntity u : List.of(teacher, student, classmate, admin)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient()
                .when(attemptRepository.findOpenMessagingBlockingAttempts(anyString()))
                .thenReturn(List.of());
        lenient()
                .when(messageRepository.save(any(ChatMessageEntity.class)))
                .thenAnswer(
                        inv -> {
                            ChatMessageEntity row = inv.getArgument(0);
                            ReflectionTestUtils.setField(row, "id", 42L);
                            return row;
                        });
    }

    private void lock(UserEntity u) {
        when(attemptRepository.findOpenMessagingBlockingAttempts(u.getEmail()))
                .thenReturn(List.of(openAttempt(Duration.ofMinutes(5), 30)));
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    // ── Sending ──────────────────────────────────────────────────────────────

    @Test
    void teacherToStudentIsStoredAndDeliveredToBoth() {
        ChatMessageDto sent = chatService.send(teacher.getEmail(), student.getEmail(), "  Hello  ");

        assertThat(sent.id()).isEqualTo(42L);
        assertThat(sent.sender()).isEqualTo(teacher.getEmail());
        assertThat(sent.recipient()).isEqualTo(student.getEmail());
        assertThat(sent.content()).isEqualTo("Hello");
        verify(messaging).convertAndSendToUser(teacher.getEmail(), ChatService.MESSAGE_QUEUE, sent);
        verify(messaging).convertAndSendToUser(student.getEmail(), ChatService.MESSAGE_QUEUE, sent);
    }

    @Test
    void studentToTeacherIsAllowed() {
        ChatMessageDto sent = chatService.send(student.getEmail(), teacher.getEmail(), "Question");
        assertThat(sent.recipient()).isEqualTo(teacher.getEmail());
    }

    @Test
    void studentToStudentIsForbiddenAndNothingIsStoredOrDelivered() {
        assertThatThrownBy(() -> chatService.send(student.getEmail(), classmate.getEmail(), "hi"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(messaging);
    }

    @Test
    void unknownRecipientLooksExactlyLikeAForbiddenOne() {
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        Throwable unknown =
                catchThrown(() -> chatService.send(teacher.getEmail(), "nobody@test.com", "hi"));
        Throwable notAllowed =
                catchThrown(() -> chatService.send(student.getEmail(), classmate.getEmail(), "hi"));

        assertThat(statusOf(unknown)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ResponseStatusException) unknown).getReason())
                .isEqualTo(((ResponseStatusException) notAllowed).getReason());
    }

    @Test
    void adminOnlyAccountCannotSend() {
        assertThatThrownBy(() -> chatService.send(admin.getEmail(), student.getEmail(), "hi"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void studentInsideABlockingQuizCannotSend() {
        lock(student);

        assertThatThrownBy(() -> chatService.send(student.getEmail(), teacher.getEmail(), "help"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(messaging);
    }

    @Test
    void messageToAStudentMidQuizIsKeptButNotPushedToThem() {
        lock(student);

        ChatMessageDto sent = chatService.send(teacher.getEmail(), student.getEmail(), "later");

        verify(messageRepository).save(any());
        verify(messaging).convertAndSendToUser(teacher.getEmail(), ChatService.MESSAGE_QUEUE, sent);
        verify(messaging, never())
                .convertAndSendToUser(eq(student.getEmail()), anyString(), any(Object.class));
    }

    @Test
    void teacherPreviewingABlockingQuizIsNotLocked() {
        lenient()
                .when(attemptRepository.findOpenMessagingBlockingAttempts(teacher.getEmail()))
                .thenReturn(List.of(openAttempt(Duration.ofMinutes(1), 30)));

        assertThat(chatService.status(teacher.getEmail()).available()).isTrue();
        chatService.send(teacher.getEmail(), student.getEmail(), "fine");
    }

    @Test
    void blankAndOversizedMessagesAreRejected() {
        assertThatThrownBy(() -> chatService.send(teacher.getEmail(), student.getEmail(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.send(teacher.getEmail(), student.getEmail(), null))
                .isInstanceOf(IllegalArgumentException.class);
        String tooLong = "x".repeat(ChatService.MAX_CONTENT_LENGTH + 1);
        assertThatThrownBy(() -> chatService.send(teacher.getEmail(), student.getEmail(), tooLong))
                .isInstanceOf(IllegalArgumentException.class);

        verify(messageRepository, never()).save(any());
    }

    // ── Reading ──────────────────────────────────────────────────────────────

    @Test
    void historyIsRefusedWhileLocked() {
        lock(student);

        assertThatThrownBy(() -> chatService.history(student.getEmail()))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));
        verify(messageRepository, never()).findChatHistory(anyString());
    }

    @Test
    void contactsAreRefusedWhileLocked() {
        lock(student);

        assertThatThrownBy(() -> chatService.contacts(student.getEmail()))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));
    }

    @Test
    void historyOmitsLegacyStudentToStudentThreads() {
        ChatMessageEntity withTeacher =
                new ChatMessageEntity(teacher.getEmail(), student.getEmail(), "from teacher", true);
        ChatMessageEntity withClassmate =
                new ChatMessageEntity(classmate.getEmail(), student.getEmail(), "psst", true);
        when(messageRepository.findChatHistory(student.getEmail()))
                .thenReturn(List.of(withTeacher, withClassmate));
        when(userRepository.findAllByEmailIn(any())).thenReturn(List.of(teacher, classmate));

        List<ChatMessageDto> history = chatService.history(student.getEmail());

        assertThat(history).extracting(ChatMessageDto::content).containsExactly("from teacher");
    }

    @Test
    void historyMatchesPartnersCaseInsensitively() {
        ChatMessageEntity legacy =
                new ChatMessageEntity(student.getEmail(), "Teacher@Test.com", "old", true);
        when(messageRepository.findChatHistory(student.getEmail())).thenReturn(List.of(legacy));
        when(userRepository.findAllByEmailIn(any())).thenReturn(List.of(teacher));

        assertThat(chatService.history(student.getEmail())).hasSize(1);
    }

    // ── Status ───────────────────────────────────────────────────────────────

    @Test
    void statusReportsTheLockAndWhenItLapses() {
        lock(student);

        ChatStatusDto status = chatService.status(student.getEmail());

        assertThat(status.available()).isFalse();
        assertThat(status.reason()).isEqualTo(ChatStatusDto.QUIZ_IN_PROGRESS);
        assertThat(status.lockedUntil()).isNotNull();
    }

    @Test
    void statusIsOpenOnceTheQuizTimeHasRunOut() {
        when(attemptRepository.findOpenMessagingBlockingAttempts(student.getEmail()))
                .thenReturn(List.of(openAttempt(Duration.ofHours(3), 30)));

        assertThat(chatService.status(student.getEmail())).isEqualTo(ChatStatusDto.open());
    }

    @Test
    void statusForAdminOnlyIsNotPermitted() {
        assertThat(chatService.status(admin.getEmail())).isEqualTo(ChatStatusDto.notPermitted());
    }

    private static Throwable catchThrown(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("Expected an exception");
    }
}
