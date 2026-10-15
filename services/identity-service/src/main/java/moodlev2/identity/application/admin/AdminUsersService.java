package moodlev2.identity.application.admin;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import moodlev2.identity.api.Role;
import moodlev2.identity.api.UserDeletedEvent;
import moodlev2.identity.api.UserEmailChangedEvent;
import moodlev2.identity.application.auth.SessionService;
import moodlev2.identity.infrastructure.persistence.ClassRepository;
import moodlev2.identity.infrastructure.persistence.SpringDataUserRepository;
import moodlev2.identity.infrastructure.persistence.entity.ClassEntity;
import moodlev2.identity.infrastructure.persistence.entity.UserEntity;
import moodlev2.identity.shared.exception.NotFoundException;
import moodlev2.identity.web.admin.dto.AdminStudentDto;
import moodlev2.identity.web.admin.dto.UpdateStudentRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private final SpringDataUserRepository userRepository;
    private final ClassRepository classRepository;
    private final ApplicationEventPublisher events;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<AdminStudentDto> getAllStudents() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.STUDENT))
                .map(this::mapToDto)
                .toList();
    }

    /**
     * Edits a user's name, email and class. A new email must be free; when it changes, other
     * modules that refer to the user by email follow it (see {@link UserEmailChangedEvent}) and
     * every session is signed out, since open sessions and sockets still carry the old address.
     */
    @Transactional
    public void updateStudent(Long id, UpdateStudentRequest request, String actorEmail) {
        UserEntity user = findManageable(id, actorEmail);

        String oldEmail = user.getEmail();
        String newEmail = normalizeEmail(request.email());
        boolean emailChanged = !newEmail.equals(oldEmail);

        if (emailChanged
                && userRepository
                        .findByEmail(newEmail)
                        .filter(other -> !other.getId().equals(user.getId()))
                        .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That email is already in use.");
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(newEmail);

        if (request.classId() != null) {
            ClassEntity clazz =
                    classRepository
                            .findById(request.classId())
                            .orElseThrow(() -> new NotFoundException("Class not found"));
            user.setClazz(clazz);
        } else {
            user.setClazz(null);
        }

        userRepository.save(user);

        if (emailChanged) {
            events.publishEvent(new UserEmailChangedEvent(user.getId(), oldEmail, newEmail));
            sessionService.revokeAll(user.getId());
        }
    }

    /**
     * Removes the second factor, e.g. for a user who lost their device. Anyone already signed in
     * with the old factor is signed out, so the reset cannot be used to ride an existing session.
     */
    @Transactional
    public void disableTwoFactor(Long id, String actorEmail) {
        UserEntity user = findManageable(id, actorEmail);

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
        sessionService.revokeAll(user.getId());
    }

    /**
     * Deletes an account. Other modules remove what they keep about the person (see {@link
     * UserDeletedEvent}); chat, for one, must not hand the conversations to whoever registers that
     * address next.
     */
    @Transactional
    public void deleteUser(Long id, String actorEmail) {
        UserEntity user = findManageable(id, actorEmail);

        events.publishEvent(new UserDeletedEvent(user.getId(), user.getEmail()));
        sessionService.revokeAll(user.getId());
        userRepository.delete(user);
    }

    /**
     * This screen manages student and teacher accounts. An admin cannot use it on their own account
     * (they could lock themselves out) or on another admin's (one admin must not be able to take
     * over or remove another).
     */
    private UserEntity findManageable(Long id, String actorEmail) {
        UserEntity user =
                userRepository
                        .findById(id)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        if (actorEmail != null && actorEmail.equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You cannot change your own account here.");
        }
        if (user.getRoles() != null && user.getRoles().contains(Role.ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Administrator accounts cannot be changed here.");
        }
        return user;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private AdminStudentDto mapToDto(UserEntity user) {
        String className = (user.getClazz() != null) ? user.getClazz().getName() : "-";
        Long classId = (user.getClazz() != null) ? user.getClazz().getId() : null;

        return new AdminStudentDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                className,
                classId,
                user.isTwoFaEnabled());
    }
}
