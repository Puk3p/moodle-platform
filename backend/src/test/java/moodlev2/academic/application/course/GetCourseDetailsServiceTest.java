package moodlev2.academic.application.course;

import static moodlev2.support.Fixtures.course;
import static moodlev2.support.Fixtures.person;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import moodlev2.academic.directory.Role;
import moodlev2.academic.directory.UserSummary;
import moodlev2.academic.infrastructure.persistence.CourseRepository;
import moodlev2.academic.infrastructure.persistence.entity.CourseEntity;
import moodlev2.academic.web.course.dto.CourseDetailsResponse;
import moodlev2.shared.exception.NotFoundException;
import moodlev2.support.FakeDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetCourseDetailsServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseQuizzes quizCatalog;

    private GetCourseDetailsService service;

    private final CourseEntity course = course(10, "CS101");
    private final FakeDirectory directory = new FakeDirectory();
    private final UserSummary student = directory.add(person(1, "student@test.com", Role.STUDENT));
    private final UserSummary teacher = directory.add(person(2, "teacher@test.com", Role.TEACHER));

    @BeforeEach
    void setUp() {
        service =
                new GetCourseDetailsService(
                        courseRepository,
                        directory,
                        quizCatalog,
                        new CourseAccess(courseRepository));
        lenient().when(courseRepository.findById(10L)).thenReturn(Optional.of(course));
        lenient().when(courseRepository.findByCode("CS101")).thenReturn(Optional.of(course));
    }

    @Test
    void studentOutsideTheCourseCannotReadItById() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(false);

        assertThatThrownBy(() -> service.getCourseDetails("10", student.email()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void studentOutsideTheCourseCannotReadItByCode() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(false);

        assertThatThrownBy(() -> service.getCourseDetails("cs101", student.email()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void notMineLooksExactlyLikeDoesNotExist() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(false);
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        Throwable notMine = catchThrown(() -> service.getCourseDetails("10", student.email()));
        Throwable missing = catchThrown(() -> service.getCourseDetails("99", student.email()));

        assertThat(notMine).isInstanceOf(NotFoundException.class);
        assertThat(missing).isInstanceOf(NotFoundException.class);
        assertThat(notMine.getMessage()).isEqualTo(missing.getMessage());
    }

    @Test
    void memberStudentSeesTheCourse() {
        when(courseRepository.isMember(10L, 1L, null)).thenReturn(true);

        CourseDetailsResponse details = service.getCourseDetails("10", student.email());

        assertThat(details.courseCode()).isEqualTo("CS101");
    }

    @Test
    void teacherSeesAnyCourse() {
        CourseDetailsResponse details = service.getCourseDetails("CS101", teacher.email());

        assertThat(details.courseCode()).isEqualTo("CS101");
    }

    @Test
    void absurdlyLongNumericIdIsNotFoundRatherThanAServerError() {
        when(courseRepository.findByCode("99999999999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.getCourseDetails("99999999999999999999999", teacher.email()))
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
