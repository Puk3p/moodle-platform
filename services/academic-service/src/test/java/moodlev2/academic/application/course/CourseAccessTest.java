package moodlev2.academic.application.course;

import static moodlev2.academic.support.Fixtures.course;
import static moodlev2.academic.support.Fixtures.person;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import moodlev2.academic.clients.Role;
import moodlev2.academic.clients.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.shared.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseAccessTest {

    @Mock private CourseRepository courseRepository;

    @InjectMocks private CourseAccess courseAccess;

    private final CourseEntity course = course(10, "CS101");
    private final UserSummary student = person(1, "student@test.com", Role.STUDENT);

    @Test
    void teachersAndAdminsManageEveryCourseWithoutALookup() {
        UserSummary teacher = person(2, "teacher@test.com", Role.TEACHER);
        UserSummary admin = person(3, "admin@test.com", Role.ADMIN);

        assertThatCode(() -> courseAccess.requireMember(course, teacher))
                .doesNotThrowAnyException();
        assertThatCode(() -> courseAccess.requireMember(course, admin)).doesNotThrowAnyException();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void enrolledOrClassAssignedStudentIsAMember() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(true);

        assertThatCode(() -> courseAccess.requireMember(course, student))
                .doesNotThrowAnyException();
    }

    @Test
    void outsiderStudentGetsNotFoundSoCoursesCannotBeProbed() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(false);

        assertThatThrownBy(() -> courseAccess.requireMember(course, student))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Course not found");
    }

    @Test
    void userWithoutRolesIsNotStaff() {
        UserSummary nobody = person(4, "nobody@test.com");
        when(courseRepository.isMember(10L, 4L, null)).thenReturn(false);

        assertThat(CourseAccess.isStaff(nobody)).isFalse();
        assertThat(courseAccess.isMember(course, nobody)).isFalse();
    }

    @Test
    void classIsPassedSoClassAssignedStudentsCount() {
        UserSummary inClass = person(5, "c@test.com", 7L, Role.STUDENT);
        when(courseRepository.isMember(10L, 5L, 7L)).thenReturn(true);

        assertThat(courseAccess.isMember(course, inClass)).isTrue();
    }

    @Test
    void missingCourseIsNeverAMembership() {
        assertThat(courseAccess.isMember(null, student)).isFalse();
    }
}
