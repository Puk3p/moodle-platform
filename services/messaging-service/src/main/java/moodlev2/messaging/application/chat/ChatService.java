package moodlev2.messaging.application.chat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.messaging.clients.Course;
import moodlev2.messaging.clients.Directory;
import moodlev2.messaging.clients.ExamLocks;
import moodlev2.messaging.clients.Membership;
import moodlev2.messaging.clients.Person;
import moodlev2.messaging.infrastructure.persistence.ChatMessageRepository;
import moodlev2.messaging.infrastructure.persistence.entity.ChatMessageEntity;
import moodlev2.messaging.web.chat.dto.ChatContactDto;
import moodlev2.messaging.web.chat.dto.ChatMessageDto;
import moodlev2.messaging.web.chat.dto.ChatReadDto;
import moodlev2.messaging.web.chat.dto.ChatStatusDto;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Teacher-student messaging. Every entry point re-checks {@link ChatAccessRules} against the
 * database, so hiding the chat in the UI is a convenience, not the enforcement: a student inside a
 * messaging-blocking quiz gets 423 from each endpoint and receives no live pushes.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    static final int MAX_CONTENT_LENGTH = 2000;

    /** Per-user queues; clients subscribe to them as {@code /user/queue/...}. */
    public static final String MESSAGE_QUEUE = "/queue/private";

    public static final String STATUS_QUEUE = "/queue/chat-status";

    public static final String READ_QUEUE = "/queue/chat-read";

    private static final String NOT_PERMITTED_MESSAGE =
            "Messaging is only available between teachers and students.";

    private final ChatMessageRepository messageRepository;
    private final Directory directory;
    private final ExamLocks examLocks;
    private final Membership membership;
    private final SimpMessagingTemplate messaging;

    @Transactional(readOnly = true)
    public ChatStatusDto status(String email) {
        Optional<Person> user =
                directory.findUserByEmail(email).filter(ChatAccessRules::canUseChat);
        if (user.isEmpty()) {
            return ChatStatusDto.notPermitted();
        }
        return lockedUntil(user.get()).map(ChatStatusDto::lockedUntil).orElse(ChatStatusDto.open());
    }

    /** Pushes the current status to all of the user's open sockets. */
    public void pushStatus(String email) {
        messaging.convertAndSendToUser(email, STATUS_QUEUE, status(email));
    }

    /**
     * A student's teachers or a teacher's students, via shared courses (direct enrollment or a
     * class assigned to the course), plus anyone already in a conversation with the caller.
     */
    @Transactional(readOnly = true)
    public List<ChatContactDto> contacts(String email) {
        Person me = requireChatUser(email);

        Map<Long, Person> people = new LinkedHashMap<>();
        Map<Long, Set<String>> sharedCourses = new HashMap<>();

        if (ChatAccessRules.isTeacher(me)) {
            collectStudentsOf(me, people, sharedCourses);
        } else {
            Map<Long, List<String>> codesByTeacher = new LinkedHashMap<>();
            for (Course course : membership.coursesOfStudent(me.id())) {
                if (course.teacherId() != null) {
                    codesByTeacher
                            .computeIfAbsent(course.teacherId(), k -> new ArrayList<>())
                            .add(course.code());
                }
            }
            for (Person teacher : directory.findUsers(codesByTeacher.keySet())) {
                codesByTeacher
                        .get(teacher.id())
                        .forEach(code -> add(people, sharedCourses, teacher, code));
            }
        }

        List<String> partners = messageRepository.findConversationPartners(me.email());
        if (!partners.isEmpty()) {
            directory.findUsersByEmail(partners).forEach(u -> people.putIfAbsent(u.id(), u));
        }

        return people.values().stream()
                .filter(u -> ChatAccessRules.mayMessage(me, u))
                .sorted(
                        Comparator.comparing(Person::lastName, String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(Person::firstName, String.CASE_INSENSITIVE_ORDER))
                .map(
                        u ->
                                new ChatContactDto(
                                        u.email(),
                                        u.firstName(),
                                        u.lastName(),
                                        ChatAccessRules.isTeacher(u) ? "TEACHER" : "STUDENT",
                                        List.copyOf(sharedCourses.getOrDefault(u.id(), Set.of()))))
                .toList();
    }

    /**
     * The caller's conversations, restricted to partners they may message today. Older
     * student-student threads from before this rule existed are left in the database but no longer
     * served.
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> history(String email) {
        Person me = requireChatUser(email);
        List<ChatMessageEntity> rows = messageRepository.findChatHistory(me.email());

        Set<String> partnerEmails =
                rows.stream()
                        .map(m -> otherParty(m, me.email()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (partnerEmails.isEmpty()) {
            return List.of();
        }

        // MySQL compares these case-insensitively, so the Java side must as well.
        Set<String> allowed =
                directory.findUsersByEmail(partnerEmails).stream()
                        .filter(u -> ChatAccessRules.mayMessage(me, u))
                        .map(u -> normalize(u.email()))
                        .collect(Collectors.toSet());

        return rows.stream()
                .filter(m -> allowed.contains(normalize(otherParty(m, me.email()))))
                .map(m -> toDto(m, me.email()))
                .toList();
    }

    /**
     * Stores a message and delivers it live to both participants' open sockets. The recipient's
     * live copy is withheld while they are locked by a quiz; it is in their history once the
     * attempt ends.
     */
    public ChatMessageDto send(String senderEmail, String recipientEmail, String content) {
        Person sender = requireChatUser(senderEmail);

        String body = content == null ? "" : content.strip();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty.");
        }
        if (body.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Message is too long (maximum " + MAX_CONTENT_LENGTH + " characters).");
        }

        Person recipient =
                recipientEmail == null
                        ? null
                        : directory.findUserByEmail(recipientEmail.strip()).orElse(null);
        // One answer for "no such account" and "not allowed", so this cannot be used to probe
        // which email addresses are registered.
        if (recipient == null || !ChatAccessRules.mayMessage(sender, recipient)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, NOT_PERMITTED_MESSAGE);
        }

        ChatMessageEntity row =
                messageRepository.save(
                        new ChatMessageEntity(sender.email(), recipient.email(), body, true));
        ChatMessageDto dto = toDto(row, sender.email());

        // The sender's other tabs; the calling tab also has it from the response and de-duplicates
        // by id.
        messaging.convertAndSendToUser(sender.email(), MESSAGE_QUEUE, dto);
        if (lockedUntil(recipient).isEmpty()) {
            messaging.convertAndSendToUser(
                    recipient.email(), MESSAGE_QUEUE, toDto(row, recipient.email()));
        }
        return dto;
    }

    /**
     * Records that the caller has read what {@code partnerEmail} sent them, up to message {@code
     * upToId}, and tells the caller's other tabs. Only rows addressed to the caller are touched, so
     * a caller cannot mark anyone else's messages.
     */
    @Transactional
    public void markRead(String email, String partnerEmail, Long upToId) {
        Person me = requireChatUser(email);
        if (partnerEmail == null || partnerEmail.isBlank() || upToId == null) {
            throw new IllegalArgumentException("A conversation and a message id are required.");
        }
        String partner = partnerEmail.strip();
        int updated = messageRepository.markRead(me.email(), partner, upToId, LocalDateTime.now());
        if (updated > 0) {
            messaging.convertAndSendToUser(
                    me.email(), READ_QUEUE, new ChatReadDto(partner, upToId));
        }
    }

    private Person requireChatUser(String email) {
        Person user =
                directory
                        .findUserByEmail(email)
                        .filter(ChatAccessRules::canUseChat)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.FORBIDDEN, NOT_PERMITTED_MESSAGE));
        if (lockedUntil(user).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED, "Messaging is unavailable while you are taking a quiz.");
        }
        return user;
    }

    private Optional<Instant> lockedUntil(Person user) {
        // Teachers are never locked: they start attempts only to preview their own quizzes.
        if (!ChatAccessRules.isStudent(user)) {
            return Optional.empty();
        }
        return ChatAccessRules.lockedUntil(
                examLocks.openMessagingBlockingAttempts(user.id()), Instant.now());
    }

    /** Everyone in the courses this teacher teaches, with the codes of the courses they share. */
    private void collectStudentsOf(
            Person teacher, Map<Long, Person> people, Map<Long, Set<String>> courses) {
        for (Course course : membership.coursesTaughtBy(teacher.id())) {
            for (Person student : membership.studentsOf(course.id())) {
                add(people, courses, student, course.code());
            }
        }
    }

    private static void add(
            Map<Long, Person> people,
            Map<Long, Set<String>> courses,
            Person user,
            String courseCode) {
        people.putIfAbsent(user.id(), user);
        courses.computeIfAbsent(user.id(), k -> new TreeSet<>()).add(courseCode);
    }

    private static String otherParty(ChatMessageEntity m, String me) {
        return me.equalsIgnoreCase(m.getSender()) ? m.getRecipient() : m.getSender();
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** {@code viewer} decides {@code read}: their own messages are always read to them. */
    static ChatMessageDto toDto(ChatMessageEntity m, String viewer) {
        // Stored as server-local wall time (LocalDateTime); sent as an absolute instant so every
        // client renders it in its own zone.
        Instant at =
                m.getTimestamp() == null
                        ? null
                        : m.getTimestamp().atZone(ZoneId.systemDefault()).toInstant();
        boolean read = m.getReadAt() != null || viewer.equalsIgnoreCase(m.getSender());
        return new ChatMessageDto(
                m.getId(), m.getSender(), m.getRecipient(), m.getContent(), at, read);
    }
}
