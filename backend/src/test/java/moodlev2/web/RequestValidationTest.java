package moodlev2.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import moodlev2.web.admin.AdminUsersController;
import moodlev2.web.admin.dto.UpdateStudentRequest;
import moodlev2.web.course.CourseController;
import moodlev2.web.course.dto.CreateCourseDto;
import moodlev2.web.quiz.QuizController;
import moodlev2.web.quiz.dto.CreateQuizDto;
import moodlev2.web.quiz.dto.QuizSubmissionDto;
import moodlev2.web.resource.ResourceController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Constraints only run when the controller parameter is marked {@code @Valid}, so both halves are
 * checked here: the DTO rules themselves, and that the endpoints actually apply them.
 */
class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    private static <T> Set<String> invalidFields(T bean) {
        return validator.validate(bean).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    // ── DTO rules ────────────────────────────────────────────────────────────

    @Test
    void adminUserEditRequiresAWellFormedEmailAndNames() {
        assertThat(invalidFields(new UpdateStudentRequest("Ana", "Pop", "ana@test.com", null)))
                .isEmpty();
        assertThat(invalidFields(new UpdateStudentRequest("Ana", "Pop", "not-an-email", null)))
                .containsExactly("email");
        assertThat(invalidFields(new UpdateStudentRequest(" ", null, null, null)))
                .containsExactlyInAnyOrder("firstName", "lastName", "email");
        assertThat(
                        invalidFields(
                                new UpdateStudentRequest(
                                        "A".repeat(101), "Pop", "ana@test.com", null)))
                .containsExactly("firstName");
    }

    @Test
    void quizSubmissionNeedsAnAttemptAndABoundedAnswerList() {
        List<QuizSubmissionDto.AnswerDto> tooMany =
                Collections.nCopies(501, new QuizSubmissionDto.AnswerDto(1L, 1L, null, null));

        assertThat(invalidFields(new QuizSubmissionDto(1L, List.of()))).isEmpty();
        assertThat(invalidFields(new QuizSubmissionDto(1L, tooMany))).containsExactly("answers");
        assertThat(invalidFields(new QuizSubmissionDto(null, null)))
                .containsExactlyInAnyOrder("attemptId", "answers");
    }

    @Test
    void quizNeedsATitle() {
        CreateQuizDto blank =
                new CreateQuizDto(
                        " ",
                        null,
                        1L,
                        null,
                        30,
                        50,
                        1,
                        false,
                        null,
                        null,
                        null,
                        null,
                        "MANUAL",
                        List.of(1L),
                        null,
                        null);

        assertThat(invalidFields(blank)).containsExactly("title");
    }

    @Test
    void courseNeedsCodeTitleAndTeacher() {
        assertThat(invalidFields(new CreateCourseDto("", "", null, null, null)))
                .containsExactlyInAnyOrder("code", "title", "teacherId");
    }

    // ── Wiring ───────────────────────────────────────────────────────────────

    private static Method method(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(type.getSimpleName() + "." + name));
    }

    private static boolean bodyIsValidated(Class<?> type, String name) {
        for (Parameter p : method(type, name).getParameters()) {
            if (p.isAnnotationPresent(RequestBody.class)) {
                return p.isAnnotationPresent(Valid.class);
            }
        }
        throw new AssertionError(name + " has no @RequestBody");
    }

    @Test
    void endpointsWithConstrainedBodiesValidateThem() {
        assertThat(bodyIsValidated(CourseController.class, "createCourse")).isTrue();
        assertThat(bodyIsValidated(QuizController.class, "createQuiz")).isTrue();
        assertThat(bodyIsValidated(QuizController.class, "updateQuiz")).isTrue();
        assertThat(bodyIsValidated(QuizController.class, "submitQuiz")).isTrue();
        assertThat(bodyIsValidated(AdminUsersController.class, "updateStudent")).isTrue();
    }

    @Test
    void staffOnlyCourseViewsAreGuardedOnTheMethodToo() {
        for (Method m :
                List.of(
                        method(CourseController.class, "getCoursePreview"),
                        method(CourseController.class, "getCourseResources"),
                        method(CourseController.class, "createCourse"),
                        method(ResourceController.class, "getUploadOptions"))) {
            assertThat(m.getAnnotation(PreAuthorize.class))
                    .as(m.getName())
                    .isNotNull()
                    .extracting(PreAuthorize::value)
                    .isEqualTo("hasAnyRole('TEACHER', 'ADMIN')");
        }
    }

    @Test
    void rawEntityAdminCourseEndpointIsGone() {
        // POST /api/admin/courses bound a JPA entity straight from the body; nothing called it.
        assertThat(classExists("moodlev2.web.admin.AdminUsersController")).isTrue();
        assertThat(classExists("moodlev2.web.admin.AdminController")).isFalse();
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
