package moodlev2.academic.application.profile;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import moodlev2.academic.clients.AccountSummary;
import moodlev2.academic.clients.DirectoryApi;
import moodlev2.academic.clients.Role;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.NotFoundException;
import moodlev2.academic.web.profile.dto.UserProfileDto;
import moodlev2.academic.web.profile.dto.UserProfileDto.EnrolledCourse;
import moodlev2.academic.web.profile.dto.UserProfileDto.StudentProfile;
import moodlev2.academic.web.profile.dto.UserProfileDto.TaughtCourse;
import moodlev2.academic.web.profile.dto.UserProfileDto.TeacherProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in user's profile page: the account from identity-service, and the courses they take
 * or teach from here. Lives with courses so that identity never needs to know about them.
 */
@Service
@RequiredArgsConstructor
public class GetMeService {

    private final DirectoryApi directory;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(long userId) {
        AccountSummary account =
                directory
                        .findAccount(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));
        UserSummary user = account.user();

        // Same precedence as the rest of the app: anyone who teaches is treated as a teacher.
        String role;
        StudentProfile student = null;
        TeacherProfile teacher = null;
        if (user.roles().contains(Role.TEACHER)) {
            role = Role.TEACHER.name();
            teacher = teacherProfile(user);
        } else if (user.roles().contains(Role.STUDENT)) {
            role = Role.STUDENT.name();
            student = studentProfile(user);
        } else {
            role = Role.ADMIN.name();
        }

        return new UserProfileDto(
                user.email(),
                user.firstName(),
                user.lastName(),
                role,
                account.twoFaEnabled(),
                student,
                teacher);
    }

    private StudentProfile studentProfile(UserSummary user) {
        List<CourseEntity> enrolled =
                courseRepository.findAllCoursesForStudent(user.id(), user.classId());
        Map<Long, String> teacherNames =
                directory
                        .findUsers(
                                enrolled.stream()
                                        .map(CourseEntity::getTeacherId)
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toSet()))
                        .stream()
                        .collect(Collectors.toMap(UserSummary::id, UserSummary::fullName));
        List<EnrolledCourse> courses =
                enrolled.stream()
                        .sorted(Comparator.comparing(CourseEntity::getCode))
                        .map(
                                c ->
                                        new EnrolledCourse(
                                                c.getCode(),
                                                c.getName(),
                                                c.getTerm(),
                                                teacherNames.get(c.getTeacherId())))
                        .toList();

        return new StudentProfile(String.valueOf(user.id()), user.className(), courses);
    }

    /**
     * A course's students are its direct enrollments plus every member of a class assigned to it,
     * counted once each.
     */
    private TeacherProfile teacherProfile(UserSummary user) {
        List<CourseEntity> taught = courseRepository.findAllByTeacherId(user.id());

        Set<Long> classIds =
                taught.stream().flatMap(c -> c.getClassIds().stream()).collect(Collectors.toSet());
        List<UserSummary> classMembers =
                classIds.isEmpty() ? List.of() : directory.findUsersInClasses(classIds);

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

    private static Set<Long> studentIdsOf(CourseEntity course, List<UserSummary> classMembers) {
        Set<Long> classIds = course.getClassIds();

        Set<Long> ids = new HashSet<>();
        course.getEnrollments().forEach(e -> ids.add(e.getUserId()));
        classMembers.stream()
                .filter(u -> u.classId() != null && classIds.contains(u.classId()))
                .forEach(u -> ids.add(u.id()));
        return ids;
    }
}
