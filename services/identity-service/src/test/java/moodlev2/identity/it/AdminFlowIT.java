package moodlev2.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

/**
 * Admin account management, and the events other services act on: an email change (chat history
 * follows it) and a deletion (every service drops what it keeps about the person). What they do
 * with them is checked in their own tests and end to end.
 */
class AdminFlowIT extends IntegrationTestBase {

    @Test
    void changingAStudentsEmailSignsThemOutAndIsPublished() throws Exception {
        TestUser admin = admin();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);

        String newEmail = "renamed-" + student.id() + "@it.test";
        JsonNode me = student.api().get("/api/auth/session").expect(200).json();
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
                findBy(admin.api().get("/api/admin/students").expect(200).json(), newEmail);
        assertThat(listed.get("lastName").asText()).isEqualTo("Renamed");
        assertThat(listed.get("classId").asLong()).isEqualTo(classId);

        ConsumerRecord<String, String> changed =
                awaitRecord(
                        "identity.user", "identity.UserEmailChanged", String.valueOf(student.id()));
        JsonNode payload = ApiClient.JSON.readTree(changed.value());
        assertThat(payload.get("userId").asLong()).isEqualTo(student.id());
        assertThat(payload.get("oldEmail").asText()).isEqualTo(student.email());
        assertThat(payload.get("newEmail").asText()).isEqualTo(newEmail);
        // The gateway drops its cached tokens for the user on this one.
        awaitRecord("identity.session", "identity.SessionsRevoked", String.valueOf(student.id()));
    }

    @Test
    void deletingAStudentEndsTheAccountAndIsPublished() throws Exception {
        TestUser admin = admin();
        TestUser student = student();

        admin.api().delete("/api/admin/students/" + student.id()).expect(200);

        student.api().get("/api/auth/session").expect(401);
        anonymous()
                .post(
                        "/api/auth/login",
                        Map.of("email", student.email(), "password", student.password()))
                .expect(400);
        assertThat(admin.api().get("/api/admin/students").expect(200).body())
                .doesNotContain(student.email());

        ConsumerRecord<String, String> deleted =
                awaitRecord("identity.user", "identity.UserDeleted", String.valueOf(student.id()));
        assertThat(ApiClient.JSON.readTree(deleted.value()).get("email").asText())
                .isEqualTo(student.email());
    }

    @Test
    void onlyAdminsManageAccounts() {
        TestUser teacher = teacher();
        TestUser student = student();
        teacher.api().get("/api/admin/students").expect(403);
        student.api().delete("/api/admin/students/" + teacher.id()).expect(403);
        student.api().post("/api/admin/classes", Map.of("name", "X1")).expect(403);
        student.api().get("/api/classes/list").expect(403);
        teacher.api().get("/api/classes/list").expect(200);
        anonymous().get("/api/users/sessions").expect(401);
    }

    private static JsonNode findBy(JsonNode array, String email) {
        for (JsonNode node : array) {
            if (email.equals(node.path("email").asText())) {
                return node;
            }
        }
        throw new AssertionError("no element with email=" + email + " in " + array);
    }
}
