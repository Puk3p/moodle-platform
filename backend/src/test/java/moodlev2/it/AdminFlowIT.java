package moodlev2.it;

import static moodlev2.it.CourseworkFlowIT.findBy;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Admin account management in core. What other services do about it (chat history following a
 * rename, or going with a deleted account) is driven by core's events: EventsToKafkaIT checks they
 * are published, the end-to-end suite checks the outcome.
 */
class AdminFlowIT extends IntegrationTestBase {

    @Test
    void changingAStudentsEmailSignsThemOut() {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        createCourse(teacher, classId);

        String newEmail = "renamed-" + student.id() + "@it.test";
        JsonNode me = student.api().get("/api/users/me").expect(200).json();
        admin.api()
                .put(
                        "/api/admin/students/" + student.id(),
                        Map.of(
                                "firstName",
                                me.get("firstName").asText(),
                                "lastName",
                                "Renamed",
                                "email",
                                newEmail,
                                "classId",
                                classId))
                .ok();

        // Signed out everywhere; the new address works with the old password.
        student.api().get("/api/auth/session").expect(401);
        ApiClient renamed = login(newEmail, student.password());
        JsonNode session = renamed.get("/api/auth/session").expect(200).json();
        assertThat(session.get("email").asText()).isEqualTo(newEmail);

        JsonNode listed =
                findBy(
                        admin.api().get("/api/admin/students").expect(200).json(),
                        "email",
                        newEmail);
        assertThat(listed.get("lastName").asText()).isEqualTo("Renamed");
        assertThat(listed.get("classId").asLong()).isEqualTo(classId);
    }

    @Test
    void deletingAStudentRemovesTheirDataEverywhere() {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        Course course = createCourse(teacher, classId);

        Map<String, Object> parts = new LinkedHashMap<>();
        parts.put("courseCode", course.code());
        parts.put("moduleId", course.moduleId());
        parts.put("title", "Task " + course.code());
        parts.put("type", "Assignment");
        parts.put("isVisible", "true");
        parts.put("maxGrade", 10);
        parts.put("submissionType", "TEXT");
        teacher.api().multipart("POST", "/api/resources", parts).expect(200);
        long assignmentId =
                findBy(
                                teacher.api()
                                        .get("/api/courses/" + course.code() + "/resources")
                                        .expect(200)
                                        .json(),
                                "name",
                                "Task " + course.code())
                        .get("id")
                        .asLong();
        student.api()
                .multipart(
                        "POST",
                        "/api/assignments/submit",
                        Map.of("assignmentId", assignmentId, "textResponse", "done"))
                .expect(200);

        admin.api().delete("/api/admin/students/" + student.id()).expect(200);

        student.api().get("/api/auth/session").expect(401);
        anonymous()
                .post(
                        "/api/auth/login",
                        Map.of("email", student.email(), "password", student.password()))
                .expect(400);

        assertThat(admin.api().get("/api/admin/students").expect(200).body())
                .doesNotContain(student.email());
        assertThat(
                        teacher.api()
                                .get("/api/assignments/" + assignmentId + "/overview")
                                .expect(200)
                                .body())
                .doesNotContain(student.email());
        assertThat(
                        teacher.api()
                                .get("/api/courses/" + course.code() + "/students")
                                .expect(200)
                                .body())
                .doesNotContain(student.email());
    }

    @Test
    void onlyAdminsManageAccounts() {
        TestUser teacher = teacher();
        TestUser student = student();
        teacher.api().get("/api/admin/students").expect(403);
        student.api().delete("/api/admin/students/" + teacher.id()).expect(403);
        student.api().post("/api/admin/classes", Map.of("name", "X1")).expect(403);
        anonymous().get("/api/courses").expect(401);
    }
}
