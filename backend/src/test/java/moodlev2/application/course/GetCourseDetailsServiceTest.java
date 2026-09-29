package moodlev2.application.course;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import moodlev2.common.exception.NotFoundException;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.CourseRepository;
import moodlev2.infrastructure.persistence.jpa.QuizAttemptRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.CourseEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.web.course.dto.CourseDetailsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCourseDetailsServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private SpringDataUserRepository userRepository;
    @Mock private QuizAttemptRepository attemptRepository;

    private GetCourseDetailsService service;

    private final CourseEntity course = course(10, "CS101");
    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);
    private final UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);

    @BeforeEach
    void setUp() {
        service =
                new GetCourseDetailsService(
                        courseRepository,
                        userRepository,
                        attemptRepository,
                        new CourseAccess(courseRepository));
        for (UserEntity u : List.of(student, teacher)) {
            lenient().when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient().when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        lenient().when(courseRepository.findByCode("CS101")).thenReturn(Optional.of(course));
    }

    @Test
    void studentOutsideTheCourseCannotReadItById() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCourseDetails("10", student.getEmail()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void studentOutsideTheCourseCannotReadItByCode() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCourseDetails("cs101", student.getEmail()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void notMineLooksExactlyLikeDoesNotExist() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(false);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Throwable notMine = catchThrown(() -> service.getCourseDetails("10", student.getEmail()));
        Throwable missing = catchThrown(() -> service.getCourseDetails("99", student.getEmail()));

        assertThat(notMine).isInstanceOf(NotFoundException.class);
        assertThat(missing).isInstanceOf(NotFoundException.class);
        assertThat(notMine.getMessage()).isEqualTo(missing.getMessage());
    }

    @Test
    void memberStudentSeesTheCourse() {
        when(courseRepository.isMember(10L, 1L)).thenReturn(true);

        CourseDetailsResponse details = service.getCourseDetails("10", student.getEmail());

        assertThat(details.courseCode()).isEqualTo("CS101");
    }

    @Test
    void teacherSeesAnyCourse() {
        CourseDetailsResponse details = service.getCourseDetails("CS101", teacher.getEmail());

        assertThat(details.courseCode()).isEqualTo("CS101");
    }

    @Test
    void absurdlyLongNumericIdIsNotFoundRatherThanAServerError() {
        when(courseRepository.findByCode("99999999999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.getCourseDetails(
                                        "99999999999999999999999", teacher.getEmail()))
                .isInstanceOf(NotFoundException.class);
    }

    private static Throwable catchThrown(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            return t;
        }
        throw new AssertionError("expected an exception");
    }
}
