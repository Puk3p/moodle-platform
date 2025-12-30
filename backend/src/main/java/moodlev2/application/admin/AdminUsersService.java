package moodlev2.application.admin;

import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ClassRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.admin.dto.AdminStudentDto;
import moodlev2.web.admin.dto.UpdateStudentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUsersService {

    private final SpringDataUserRepository userRepository;
    private final ClassRepository classRepository;

    @Transactional(readOnly = true)
    public List<AdminStudentDto> getAllStudents() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRoles().contains(Role.STUDENT))
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public void updateStudent(Long id, UpdateStudentRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email());

        if (request.classId() != null) {
            ClassEntity clazz = classRepository.findById(request.classId())
                    .orElseThrow(() -> new NotFoundException("Class not found"));
            user.setClazz(clazz);
        } else {
            user.setClazz(null);
        }

        userRepository.save(user);
    }

    @Transactional
    public void disableTwoFactor(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(id);
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
                user.isTwoFaEnabled()
        );
    }
}