package moodlev2.application.course;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
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
    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);

    @Test
    void teachersAndAdminsManageEveryCourseWithoutALookup() {
        UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
        UserEntity admin = user(3, "admin@test.com", Role.ADMIN);

        assertThatCode(() -> courseAccess.requireMember(course, teacher))
                .doesNotThrowAnyException();
        assertThatCode(() -> courseAccess.requireMember(course, admin)).doesNotThrowAnyException();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void enrolledOrClassAssignedStudentIsAMember() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(true);

        assertThatCode(() -> courseAccess.requireMember(course, student))
                .doesNotThrowAnyException();
    }

    @Test
    void outsiderStudentGetsNotFoundSoCoursesCannotBeProbed() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> courseAccess.requireMember(course, student))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Course not found");
    }

    @Test
    void userWithoutRolesIsNotStaff() {
        UserEntity nobody = user(4, "nobody@test.com");
        when(courseRepository.isMember(10L, 4L)).thenReturn(false);

        assertThat(CourseAccess.isStaff(nobody)).isFalse();
        assertThat(courseAccess.isMember(course, nobody)).isFalse();
    }

    @Test
    void missingCourseIsNeverAMembership() {
        assertThat(courseAccess.isMember(null, student)).isFalse();
    }
}
