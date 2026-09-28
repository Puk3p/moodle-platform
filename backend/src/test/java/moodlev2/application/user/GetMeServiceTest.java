package moodlev2.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ClassEntity;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.EnrollmentEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.user.dto.UserProfileDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMeServiceTest {

    @Mock private SpringDataUserRepository userRepository;
    @Mock private CourseRepository courseRepository;
    @InjectMocks private GetMeService service;

    private static UserEntity user(long id, String email, Role... roles) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail(email);
        u.setFirstName("First" + id);
        u.setLastName("Last" + id);
        u.setRoles(new HashSet<>(List.of(roles)));
        return u;
    }

    private static ClassEntity clazz(long id, String name) {
        ClassEntity c = new ClassEntity();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private static CourseEntity course(String code, UserEntity teacher) {
        CourseEntity c = new CourseEntity();
        c.setCode(code);
        c.setName(code + " name");
        c.setTerm("Fall 2026");
        c.setStatus("PUBLISHED");
        c.setTeacher(teacher);
        return c;
    }

    private static void enroll(CourseEntity course, UserEntity student) {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setCourse(course);
        e.setUser(student);
        course.getEnrollments().add(e);
    }

    @Test
    void teacherGetsTaughtCoursesAndNoStudentFields() {
        UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(courseRepository.findAllByTeacherId(2L)).thenReturn(List.of());

        UserProfileDto me = service.getCurrentUserProfile(teacher.getEmail());

        assertThat(me.role()).isEqualTo("TEACHER");
        assertThat(me.student()).isNull();
        assertThat(me.teacher()).isNotNull();
        assertThat(me.teacher().courses()).isEmpty();
        assertThat(me.teacher().studentCount()).isZero();
    }

    @Test
    void aStudentEnrolledAndInAnAssignedClassIsCountedOnce() {
        UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
        ClassEntity group = clazz(9, "1209A");
        UserEntity alex = user(1, "student@test.com", Role.STUDENT);
        alex.setClazz(group);
        UserEntity sam = user(4, "student4@test.com", Role.STUDENT);

        CourseEntity cs201 = course("CS201", teacher);
        enroll(cs201, alex);
        enroll(cs201, sam);
        cs201.setAssignedClasses(Set.of(group));
        CourseEntity cs350 = course("CS350", teacher);
        enroll(cs350, alex);

        when(userRepository.findByEmail(teacher.getEmail())).thenReturn(Optional.of(teacher));
        when(courseRepository.findAllByTeacherId(2L)).thenReturn(List.of(cs350, cs201));
        when(userRepository.findAllByClazzIdIn(anyCollection())).thenReturn(List.of(alex));

        UserProfileDto.TeacherProfile profile =
                service.getCurrentUserProfile(teacher.getEmail()).teacher();

        assertThat(profile.courses())
                .extracting(UserProfileDto.TaughtCourse::code)
                .containsExactly("CS201", "CS350");
        assertThat(profile.courses().get(0).studentCount()).isEqualTo(2);
        assertThat(profile.courses().get(1).studentCount()).isEqualTo(1);
        assertThat(profile.studentCount()).isEqualTo(2);
    }

    @Test
    void studentGetsClassIdAndCoursesButNoTeacherFields() {
        UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
        UserEntity alex = user(1, "student@test.com", Role.STUDENT);
        alex.setClazz(clazz(9, "1209A"));
        when(userRepository.findByEmail(alex.getEmail())).thenReturn(Optional.of(alex));
        when(courseRepository.findAllCoursesForStudent(1L))
                .thenReturn(List.of(course("CS201", teacher), course("LAB1", null)));

        UserProfileDto me = service.getCurrentUserProfile(alex.getEmail());

        assertThat(me.role()).isEqualTo("STUDENT");
        assertThat(me.teacher()).isNull();
        assertThat(me.student().className()).isEqualTo("1209A");
        assertThat(me.student().studentId()).isEqualTo("1");
        assertThat(me.student().courses())
                .extracting(UserProfileDto.EnrolledCourse::teacherName)
                .containsExactly("First2 Last2", null);
    }

    @Test
    void studentWithoutAClassHasNoClassNameRatherThanAPlaceholder() {
        UserEntity sam = user(4, "student4@test.com", Role.STUDENT);
        when(userRepository.findByEmail(sam.getEmail())).thenReturn(Optional.of(sam));
        when(courseRepository.findAllCoursesForStudent(4L)).thenReturn(List.of());

        assertThat(service.getCurrentUserProfile(sam.getEmail()).student().className()).isNull();
    }

    @Test
    void adminOnlyAccountGetsNeitherBlock() {
        UserEntity admin = user(3, "admin@test.com", Role.ADMIN);
        when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));

        UserProfileDto me = service.getCurrentUserProfile(admin.getEmail());

        assertThat(me.role()).isEqualTo("ADMIN");
        assertThat(me.student()).isNull();
        assertThat(me.teacher()).isNull();
    }
}
