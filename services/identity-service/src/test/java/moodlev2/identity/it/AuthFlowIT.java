package moodlev2.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Sign-up, sign-in, the session cookie, CSRF and sign-out, end to end. */
class AuthFlowIT extends IntegrationTestBase {

    @Test
    void registerStartsASessionThatLogoutEnds() {
        TestUser user = student();

        JsonNode session = user.api().get("/api/auth/session").expect(200).json();
        assertThat(session.get("email").asText()).isEqualTo(user.email());
        assertThat(session.get("roles").toString()).contains("STUDENT");
        assertThat(user.api().hasCookie("mdl_session")).isTrue();

        user.api().post("/api/auth/logout", null).expect(204);

        assertThat(user.api().hasCookie("mdl_session")).isFalse();
        user.api().get("/api/auth/session").expect(401);
    }

    @Test
    void loginWithTheRightPasswordOnly() {
        TestUser user = student();

        anonymous()
                .post("/api/auth/login", Map.of("email", user.email(), "password", "wrong-pass"))
                .expect(400);

        ApiClient browser = login(user.email(), user.password());
        browser.get("/api/auth/session").expect(200);
        browser.get("/api/users/sessions").expect(200);
    }

    @Test
    void stateChangingCallsNeedTheCsrfHeader() {
        // A raw request with no XSRF cookie/header, as a cross-site form post would be.
        ApiClient bare = anonymous();
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        try {
            var response =
                    http.send(
                            java.net.http.HttpRequest.newBuilder(
                                            java.net.URI.create(
                                                    "http://127.0.0.1:" + port + "/api/auth/login"))
                                    .header("Content-Type", "application/json")
                                    .POST(
                                            java.net.http.HttpRequest.BodyPublishers.ofString(
                                                    "{\"email\":\"x@it.test\",\"password\":\"x\"}"))
                                    .build(),
                            java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(403);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        bare.get("/api/auth/session").expect(401);
    }

    @Test
    void changingThePasswordSignsOutOtherSessions() {
        TestUser user = student();
        ApiClient otherBrowser = login(user.email(), user.password());
        otherBrowser.get("/api/auth/session").expect(200);

        user.api()
                .post(
                        "/api/users/change-password",
                        Map.of(
                                "currentPassword",
                                user.password(),
                                "newPassword",
                                "N3w-password-" + user.id()))
                .ok();

        otherBrowser.get("/api/auth/session").expect(401);
        login(user.email(), "N3w-password-" + user.id()).get("/api/auth/session").expect(200);
    }

    @Test
    void forgotPasswordAlwaysAnswersTheSame() {
        // Must not reveal whether an account exists; mail delivery failing must not leak either.
        anonymous()
                .post("/api/auth/forgot-password", Map.of("email", "nobody@it.test"))
                .expect(200);
        TestUser user = student();
        anonymous().post("/api/auth/forgot-password", Map.of("email", user.email())).expect(200);
    }
}
