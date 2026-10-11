package moodlev2.messaging.application.chat;

import static moodlev2.messaging.application.chat.ChatTestData.openAttempt;
import static moodlev2.messaging.application.chat.ChatTestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.assessment.infrastructure.persistence.QuizAttemptRepository;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserSummary;
import moodlev2.messaging.infrastructure.persistence.ChatMessageRepository;
import moodlev2.messaging.infrastructure.persistence.entity.ChatMessageEntity;
import moodlev2.messaging.web.chat.dto.ChatMessageDto;
import moodlev2.messaging.web.chat.dto.ChatReadDto;
import moodlev2.messaging.web.chat.dto.ChatStatusDto;
import moodlev2.support.FakeDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository messageRepository;
    @Mock private QuizAttemptRepository attemptRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private SimpMessagingTemplate messaging;

    private ChatService chatService;

    private final FakeDirectory directory = new FakeDirectory();
    private final UserSummary teacher = directory.add(user(1, "teacher@test.com", Role.TEACHER));
    private final UserSummary student = directory.add(user(2, "student@test.com", Role.STUDENT));
    private final UserSummary classmate = directory.add(user(3, "student4@test.com", Role.STUDENT));
    private final UserSummary admin = directory.add(user(4, "admin@test.com", Role.ADMIN));

    @BeforeEach
    void setUp() {
        chatService =
                new ChatService(
                        messageRepository,
                        directory,
                        attemptRepository,
                        courseRepository,
                        messaging);
        lenient()
                .when(attemptRepository.findOpenMessagingBlockingAttempts(anyLong()))
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

    private void lock(UserSummary u) {
        when(attemptRepository.findOpenMessagingBlockingAttempts(u.id()))
                .thenReturn(List.of(openAttempt(Duration.ofMinutes(5), 30)));
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    // ── Sending ──────────────────────────────────────────────────────────────

    @Test
    void teacherToStudentIsStoredAndDeliveredToBoth() {
        ChatMessageDto sent = chatService.send(teacher.email(), student.email(), "  Hello  ");

        assertThat(sent.id()).isEqualTo(42L);
        assertThat(sent.sender()).isEqualTo(teacher.email());
        assertThat(sent.recipient()).isEqualTo(student.email());
        assertThat(sent.content()).isEqualTo("Hello");
        assertThat(sent.read()).isTrue();
        verify(messaging).convertAndSendToUser(teacher.email(), ChatService.MESSAGE_QUEUE, sent);
        // The recipient's copy is the same message, unread to them.
        ChatMessageDto delivered =
                new ChatMessageDto(
                        sent.id(),
                        sent.sender(),
                        sent.recipient(),
                        "Hello",
                        sent.timestamp(),
                        false);
        verify(messaging)
                .convertAndSendToUser(student.email(), ChatService.MESSAGE_QUEUE, delivered);
    }

    @Test
    void studentToTeacherIsAllowed() {
        ChatMessageDto sent = chatService.send(student.email(), teacher.email(), "Question");
        assertThat(sent.recipient()).isEqualTo(teacher.email());
    }

    @Test
    void studentToStudentIsForbiddenAndNothingIsStoredOrDelivered() {
        assertThatThrownBy(() -> chatService.send(student.email(), classmate.email(), "hi"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(messaging);
    }

    @Test
    void unknownRecipientLooksExactlyLikeAForbiddenOne() {

        Throwable unknown =
                catchThrown(() -> chatService.send(teacher.email(), "nobody@test.com", "hi"));
        Throwable notAllowed =
                catchThrown(() -> chatService.send(student.email(), classmate.email(), "hi"));

        assertThat(statusOf(unknown)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((ResponseStatusException) unknown).getReason())
                .isEqualTo(((ResponseStatusException) notAllowed).getReason());
    }

    @Test
    void adminOnlyAccountCannotSend() {
        assertThatThrownBy(() -> chatService.send(admin.email(), student.email(), "hi"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void studentInsideABlockingQuizCannotSend() {
        lock(student);

        assertThatThrownBy(() -> chatService.send(student.email(), teacher.email(), "help"))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));

        verify(messageRepository, never()).save(any());
        verifyNoInteractions(messaging);
    }

    @Test
    void messageToAStudentMidQuizIsKeptButNotPushedToThem() {
        lock(student);

        ChatMessageDto sent = chatService.send(teacher.email(), student.email(), "later");

        verify(messageRepository).save(any());
        verify(messaging).convertAndSendToUser(teacher.email(), ChatService.MESSAGE_QUEUE, sent);
        verify(messaging, never())
                .convertAndSendToUser(eq(student.email()), anyString(), any(Object.class));
    }

    @Test
    void teacherPreviewingABlockingQuizIsNotLocked() {
        lenient()
                .when(attemptRepository.findOpenMessagingBlockingAttempts(teacher.id()))
                .thenReturn(List.of(openAttempt(Duration.ofMinutes(1), 30)));

        assertThat(chatService.status(teacher.email()).available()).isTrue();
        chatService.send(teacher.email(), student.email(), "fine");
    }

    @Test
    void blankAndOversizedMessagesAreRejected() {
        assertThatThrownBy(() -> chatService.send(teacher.email(), student.email(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.send(teacher.email(), student.email(), null))
                .isInstanceOf(IllegalArgumentException.class);
        String tooLong = "x".repeat(ChatService.MAX_CONTENT_LENGTH + 1);
        assertThatThrownBy(() -> chatService.send(teacher.email(), student.email(), tooLong))
                .isInstanceOf(IllegalArgumentException.class);

        verify(messageRepository, never()).save(any());
    }

    // ── Reading ──────────────────────────────────────────────────────────────

    @Test
    void historyIsRefusedWhileLocked() {
        lock(student);

        assertThatThrownBy(() -> chatService.history(student.email()))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));
        verify(messageRepository, never()).findChatHistory(anyString());
    }

    @Test
    void contactsAreRefusedWhileLocked() {
        lock(student);

        assertThatThrownBy(() -> chatService.contacts(student.email()))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.LOCKED));
    }

    @Test
    void historyOmitsLegacyStudentToStudentThreads() {
        ChatMessageEntity withTeacher =
                new ChatMessageEntity(teacher.email(), student.email(), "from teacher", true);
        ChatMessageEntity withClassmate =
                new ChatMessageEntity(classmate.email(), student.email(), "psst", true);
        when(messageRepository.findChatHistory(student.email()))
                .thenReturn(List.of(withTeacher, withClassmate));

        List<ChatMessageDto> history = chatService.history(student.email());

        assertThat(history).extracting(ChatMessageDto::content).containsExactly("from teacher");
    }

    @Test
    void historyMatchesPartnersCaseInsensitively() {
        ChatMessageEntity legacy =
                new ChatMessageEntity(student.email(), "Teacher@Test.com", "old", true);
        when(messageRepository.findChatHistory(student.email())).thenReturn(List.of(legacy));

        assertThat(chatService.history(student.email())).hasSize(1);
    }

    // ── Status ───────────────────────────────────────────────────────────────

    @Test
    void statusReportsTheLockAndWhenItLapses() {
        lock(student);

        ChatStatusDto status = chatService.status(student.email());

        assertThat(status.available()).isFalse();
        assertThat(status.reason()).isEqualTo(ChatStatusDto.QUIZ_IN_PROGRESS);
        assertThat(status.lockedUntil()).isNotNull();
    }

    @Test
    void statusIsOpenOnceTheQuizTimeHasRunOut() {
        when(attemptRepository.findOpenMessagingBlockingAttempts(student.id()))
                .thenReturn(List.of(openAttempt(Duration.ofHours(3), 30)));

        assertThat(chatService.status(student.email())).isEqualTo(ChatStatusDto.open());
    }

    @Test
    void statusForAdminOnlyIsNotPermitted() {
        assertThat(chatService.status(admin.email())).isEqualTo(ChatStatusDto.notPermitted());
    }

    private static Throwable catchThrown(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("Expected an exception");
    }

    // ── Read state ───────────────────────────────────────────────────────────

    @Test
    void historyReportsReadStateFromTheViewersSide() {
        ChatMessageEntity unreadToMe = row(1L, teacher, student, null);
        ChatMessageEntity readToMe = row(2L, teacher, student, java.time.LocalDateTime.now());
        ChatMessageEntity mine = row(3L, student, teacher, null);
        when(messageRepository.findChatHistory(student.email()))
                .thenReturn(List.of(unreadToMe, readToMe, mine));

        List<ChatMessageDto> history = chatService.history(student.email());

        assertThat(history).extracting(ChatMessageDto::read).containsExactly(false, true, true);
    }

    @Test
    void markingReadIsStoredAndSyncedToTheReadersOtherTabs() {
        when(messageRepository.markRead(eq(student.email()), eq(teacher.email()), eq(7L), any()))
                .thenReturn(2);

        chatService.markRead(student.email(), " " + teacher.email() + " ", 7L);

        verify(messaging)
                .convertAndSendToUser(
                        student.email(),
                        ChatService.READ_QUEUE,
                        new ChatReadDto(teacher.email(), 7L));
    }

    @Test
    void markingAnAlreadyReadConversationPushesNothing() {
        when(messageRepository.markRead(anyString(), anyString(), eq(7L), any())).thenReturn(0);

        chatService.markRead(student.email(), teacher.email(), 7L);

        verifyNoInteractions(messaging);
    }

    @Test
    void markingReadNeedsAConversationAndAMessage() {
        assertThatThrownBy(() -> chatService.markRead(student.email(), " ", 7L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.markRead(student.email(), teacher.email(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void studentMidQuizCannotMarkMessagesRead() {
        lock(student);

        Throwable thrown =
                catchThrown(() -> chatService.markRead(student.email(), teacher.email(), 7L));

        assertThat(statusOf(thrown)).isEqualTo(HttpStatus.LOCKED);
        verify(messageRepository, never()).markRead(anyString(), anyString(), eq(7L), any());
    }

    private static ChatMessageEntity row(
            long id, UserSummary from, UserSummary to, java.time.LocalDateTime readAt) {
        ChatMessageEntity m = new ChatMessageEntity(from.email(), to.email(), "m" + id, true);
        ReflectionTestUtils.setField(m, "id", id);
        m.setReadAt(readAt);
        return m;
    }
}
