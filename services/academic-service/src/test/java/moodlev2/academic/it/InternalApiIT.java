package moodlev2.academic.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/**
 * Who may call what. Browser-facing paths need a user token, the internal API a service token, and
 * both only when identity-service signed them; roles in the token decide the rest.
 */
class InternalApiIT extends IntegrationTestBase {

    @Test
    void theInternalApiAnswersServiceTokensOnly() {
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass();
        placeInClass(student, classId);
        Course course = createCourse(teacher, classId);
        String members = "/internal/membership/courses/" + course.id() + "/members/";

        anonymous().get(members + student.id()).expect(401);
        withToken(IDENTITY.forgedServiceToken()).get(members + student.id()).expect(401);
        // A user token is not a service token: it opens nothing internal.
        student.api().get(members + student.id()).expect(403);

        ApiClient service = withToken(IDENTITY.serviceToken());
        assertThat(service.get(members + student.id()).expect(200).body()).isEqualTo("true");
        assertThat(service.get(members + student().id()).expect(200).body()).isEqualTo("false");

        JsonNode catalog =
                service.get("/internal/catalog/courses/" + course.id()).expect(200).json();
        assertThat(catalog.get("code").asText()).isEqualTo(course.code());
        assertThat(catalog.get("teacherId").asLong()).isEqualTo(teacher.id());
    }

    @Test
    void theApiAnswersUserTokensWithTheRightRole() {
        TestUser teacher = teacher();
        TestUser student = student();

        anonymous().get("/api/courses/my-dashboard").expect(401);
        // Services act on nobody's behalf: they do not get user pages.
        withToken(IDENTITY.serviceToken()).get("/api/courses/my-dashboard").expect(403);

        student.api().get("/api/courses/my-dashboard").expect(200);
        student.api().get("/api/teacher/dashboard").expect(403);
        student.api().get("/api/courses/list").expect(403);
        teacher.api().get("/api/teacher/dashboard").expect(200);
    }

    @Test
    void theProfileCombinesTheAccountWithCourses() {
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass();
        placeInClass(student, classId);
        Course course = createCourse(teacher, classId);
        IDENTITY.enableTwoFactor(student.id());

        JsonNode me = student.api().get("/api/users/me").expect(200).json();
        assertThat(me.get("email").asText()).isEqualTo(student.email());
        assertThat(me.get("role").asText()).isEqualTo("STUDENT");
        assertThat(me.get("twoFaEnabled").asBoolean()).isTrue();
        assertThat(me.get("student").get("className").asText()).isEqualTo("C" + classId);
        assertThat(me.get("student").get("courses").toString()).contains(course.code());

        JsonNode taught = teacher.api().get("/api/users/me").expect(200).json();
        assertThat(taught.get("teacher").get("studentCount").asInt()).isEqualTo(1);
    }

    @Test
    void aRefusedServiceTokenIsReplacedWithoutTheUserNoticing() {
        TestUser student = student();
        student.api().get("/api/courses/my-dashboard").expect(200);
        int issued = IDENTITY.serviceTokensIssued();

        // identity-service restarted with new keys: the token this service holds is refused.
        IDENTITY.refuseTokensIssuedSoFar();

        student.api().get("/api/courses/my-dashboard").expect(200);
        assertThat(IDENTITY.serviceTokensIssued()).isEqualTo(issued + 1);
    }

    @Test
    void withoutTheDirectoryPagesFailFastInsteadOfGuessing() {
        TestUser student = student();
        IDENTITY.down.set(true);
        try {
            student.api().get("/api/courses/my-dashboard").expect(503);
        } finally {
            IDENTITY.down.set(false);
        }
        student.api().get("/api/courses/my-dashboard").expect(200);
    }
}
