package moodlev2.identity.web.admin;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import moodlev2.identity.web.admin.dto.UpdateStudentRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Constraints only run when the controller parameter is marked {@code @Valid}, so both halves are
 * checked here: the DTO rules themselves, and that the endpoint actually applies them.
 */
class AdminRequestValidationTest {

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
                .collect(Collectors.toSet());
    }

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
    void theEditEndpointValidatesItsBody() {
        Method update =
                Arrays.stream(AdminUsersController.class.getDeclaredMethods())
                        .filter(m -> m.getName().equals("updateStudent"))
                        .findFirst()
                        .orElseThrow();
        Parameter body =
                Arrays.stream(update.getParameters())
                        .filter(p -> p.isAnnotationPresent(RequestBody.class))
                        .findFirst()
                        .orElseThrow();
        assertThat(body.isAnnotationPresent(Valid.class)).isTrue();
    }
}
