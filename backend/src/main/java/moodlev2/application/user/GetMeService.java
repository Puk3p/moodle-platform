package moodlev2.application.user;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.user.dto.UserProfileDto;
import moodlev2.web.user.dto.UserProfileDto.EnrolledCourse;
import moodlev2.web.user.dto.UserProfileDto.StudentProfile;
import moodlev2.web.user.dto.UserProfileDto.TaughtCourse;
import moodlev2.web.user.dto.UserProfileDto.TeacherProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMeService {

    private final SpringDataUserRepository springDataUserRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentUserProfile(String email) {
        UserEntity user =
                springDataUserRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("User not found"));

        // Same precedence as the rest of the app: anyone who teaches is treated as a teacher.
        String role;
        StudentProfile student = null;
        TeacherProfile teacher = null;
        if (user.getRoles().contains(Role.TEACHER)) {
            role = Role.TEACHER.name();
            teacher = teacherProfile(user);
        } else if (user.getRoles().contains(Role.STUDENT)) {
            role = Role.STUDENT.name();
            student = studentProfile(user);
        } else {
            role = Role.ADMIN.name();
        }

        return new UserProfileDto(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role,
                user.isTwoFaEnabled(),
                student,
                teacher);
    }

    private StudentProfile studentProfile(UserEntity user) {
        List<EnrolledCourse> courses =
                courseRepository.findAllCoursesForStudent(user.getId()).stream()
                        .sorted(Comparator.comparing(CourseEntity::getCode))
                        .map(
                                c ->
                                        new EnrolledCourse(
                                                c.getCode(),
                                                c.getName(),
                                                c.getTerm(),
                                                c.getTeacher() == null
                                                        ? null
                                                        : c.getTeacher().getFirstName()
                                                                + " "
                                                                + c.getTeacher().getLastName()))
                        .toList();

        return new StudentProfile(
                String.valueOf(user.getId()),
                user.getClazz() == null ? null : user.getClazz().getName(),
                courses);
    }

    /**
     * A course's students are its direct enrollments plus every member of a class assigned to it,
     * counted once each.
     */
    private TeacherProfile teacherProfile(UserEntity user) {
        List<CourseEntity> taught = courseRepository.findAllByTeacherId(user.getId());

        Set<Long> classIds =
                taught.stream()
                        .flatMap(c -> c.getAssignedClasses().stream())
                        .map(ClassEntity::getId)
                        .collect(Collectors.toSet());
        List<UserEntity> classMembers =
                classIds.isEmpty()
                        ? List.of()
                        : springDataUserRepository.findAllByClazzIdIn(classIds);

        Set<Long> everyone = new HashSet<>();
        List<TaughtCourse> courses =
                taught.stream()
                        .sorted(Comparator.comparing(CourseEntity::getCode))
                        .map(
                                c -> {
                                    Set<Long> ids = studentIdsOf(c, classMembers);
                                    everyone.addAll(ids);
                                    return new TaughtCourse(
                                            c.getCode(),
                                            c.getName(),
                                            c.getTerm(),
                                            c.getStatus(),
                                            ids.size());
                                })
                        .toList();

        return new TeacherProfile(courses, everyone.size());
    }

    private static Set<Long> studentIdsOf(CourseEntity course, List<UserEntity> classMembers) {
        Set<Long> classIds =
                course.getAssignedClasses().stream()
                        .map(ClassEntity::getId)
                        .collect(Collectors.toSet());

        Set<Long> ids = new HashSet<>();
        course.getEnrollments().forEach(e -> ids.add(e.getUser().getId()));
        classMembers.stream()
                .filter(u -> u.getClazz() != null && classIds.contains(u.getClazz().getId()))
                .forEach(u -> ids.add(u.getId()));
        return ids;
    }
}
