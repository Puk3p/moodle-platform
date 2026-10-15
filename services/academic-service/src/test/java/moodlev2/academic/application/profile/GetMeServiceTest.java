package moodlev2.academic.application.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import moodlev2.academic.directory.Role;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.infrastructure.persistence.entity.EnrollmentEntity;
import moodlev2.academic.support.FakeDirectory;
import moodlev2.academic.web.profile.dto.UserProfileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetMeServiceTest {

    @Mock private CourseRepository courseRepository;
    private final FakeDirectory directory = new FakeDirectory();
    private GetMeService service;

    @BeforeEach
    void setUp() {
        service = new GetMeService(directory, courseRepository);
        directory.addClass(9, "1209A");
        directory.add(1, "student@test.com", 9L, Role.STUDENT);
        directory.add(2, "teacher@test.com", null, Role.TEACHER);
        directory.add(3, "admin@test.com", null, Role.ADMIN);
        directory.add(4, "student4@test.com", null, Role.STUDENT);
    }

    private static CourseEntity course(String code, Long teacherId) {
        CourseEntity c = new CourseEntity();
        c.setCode(code);
        c.setName(code + " name");
        c.setTerm("Fall 2026");
        c.setStatus("PUBLISHED");
        c.setTeacherId(teacherId);
        return c;
    }

    private static void enroll(CourseEntity course, long studentId) {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setCourse(course);
        e.setUserId(studentId);
        course.getEnrollments().add(e);
    }

    @Test
    void teacherGetsTaughtCoursesAndNoStudentFields() {
        when(courseRepository.findAllByTeacherId(2L)).thenReturn(List.of());

        UserProfileDto me = service.getProfile(2);

        assertThat(me.role()).isEqualTo("TEACHER");
        assertThat(me.email()).isEqualTo("teacher@test.com");
        assertThat(me.student()).isNull();
        assertThat(me.teacher()).isNotNull();
        assertThat(me.teacher().courses()).isEmpty();
        assertThat(me.teacher().studentCount()).isZero();
    }

    @Test
    void aStudentEnrolledAndInAnAssignedClassIsCountedOnce() {
        CourseEntity cs201 = course("CS201", 2L);
        enroll(cs201, 1);
        enroll(cs201, 4);
        cs201.setClassIds(new HashSet<>(Set.of(9L)));
        CourseEntity cs350 = course("CS350", 2L);
        enroll(cs350, 1);
        when(courseRepository.findAllByTeacherId(2L)).thenReturn(List.of(cs350, cs201));

        UserProfileDto.TeacherProfile profile = service.getProfile(2).teacher();

        assertThat(profile.courses())
                .extracting(UserProfileDto.TaughtCourse::code)
                .containsExactly("CS201", "CS350");
        assertThat(profile.courses().get(0).studentCount()).isEqualTo(2);
        assertThat(profile.courses().get(1).studentCount()).isEqualTo(1);
        assertThat(profile.studentCount()).isEqualTo(2);
    }

    @Test
    void studentGetsClassIdAndCoursesButNoTeacherFields() {
        when(courseRepository.findAllCoursesForStudent(1L, 9L))
                .thenReturn(List.of(course("CS201", 2L), course("LAB1", null)));

        UserProfileDto me = service.getProfile(1);

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
        when(courseRepository.findAllCoursesForStudent(4L, null)).thenReturn(List.of());

        assertThat(service.getProfile(4).student().className()).isNull();
    }

    @Test
    void adminOnlyAccountGetsNeitherBlock() {
        UserProfileDto me = service.getProfile(3);

        assertThat(me.role()).isEqualTo("ADMIN");
        assertThat(me.student()).isNull();
        assertThat(me.teacher()).isNull();
    }

    @Test
    void twoFactorStatusComesFromTheAccount() {
        assertThat(service.getProfile(3).twoFaEnabled()).isFalse();
        directory.enableTwoFactor(3);
        assertThat(service.getProfile(3).twoFaEnabled()).isTrue();
    }
}
