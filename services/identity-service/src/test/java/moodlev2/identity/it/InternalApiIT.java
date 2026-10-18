package moodlev2.identity.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import moodlev2.identity.infrastructure.security.InternalJwtKeys;
import org.junit.jupiter.api.Test;

/**
 * The internal API the gateway and services use: client-credentials tokens, the session-to-token
 * exchange, and scope checks. Browser credentials (cookies) never work here.
 */
class InternalApiIT extends IntegrationTestBase {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void servicesGetTokensOnlyWithTheirSecret() throws Exception {
        assertThat(token("it-service", "wrong").statusCode()).isEqualTo(401);
        assertThat(token("unknown", "it-service-secret").statusCode()).isEqualTo(401);

        HttpResponse<String> ok = token("it-service", "it-service-secret");
        assertThat(ok.statusCode()).isEqualTo(200);
        JsonNode body = ApiClient.JSON.readTree(ok.body());
        assertThat(body.get("token_type").asText()).isEqualTo("Bearer");

        SignedJWT jwt = SignedJWT.parse(body.get("access_token").asText());
        assertThat(jwt.verify(new RSASSAVerifier(jwks().getKeys().get(0).toRSAKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("service:it-service");
        assertThat(jwt.getJWTClaimsSet().getIssuer()).isEqualTo("moodle-identity");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("moodle-internal");
        assertThat(jwt.getJWTClaimsSet().getStringClaim("scope")).isEqualTo("internal");
    }

    @Test
    void theSigningKeyIsKeptForRestartsAndOtherInstances() throws Exception {
        JWKSet published = jwks();
        String tokenBefore = serviceToken();

        // What a restarted, or a second, instance loads from the same database.
        InternalJwtKeys restarted = new InternalJwtKeys("", "", jdbc);

        assertThat(restarted.publicJwks().getKeys().get(0).getKeyID())
                .isEqualTo(published.getKeys().get(0).getKeyID());
        assertThat(SignedJWT.parse(tokenBefore).verify(new RSASSAVerifier(restarted.publicKey())))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM signing_keys", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void aForgedOrMalformedTokenIsRefusedWith401() throws Exception {
        // Shaped like a real service token, signed by a key this service never published: what a
        // client still holds after a restart that generated new keys, or what an attacker sends.
        RSAKey stranger = new RSAKeyGenerator(2048).keyID("stranger").generate();
        SignedJWT forged =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("stranger").build(),
                        new JWTClaimsSet.Builder()
                                .issuer("moodle-identity")
                                .audience("moodle-internal")
                                .subject("service:it-service")
                                .claim("scope", "internal")
                                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                                .build());
        forged.sign(new RSASSASigner(stranger));

        for (String token : List.of(forged.serialize(), "not-a-jwt")) {
            HttpResponse<String> response = get("/internal/directory/users/1", token);
            assertThat(response.statusCode()).as(token).isEqualTo(401);
            // So a client knows to drop the token and get a new one.
            assertThat(response.headers().firstValue("WWW-Authenticate"))
                    .as(token)
                    .hasValueSatisfying(h -> assertThat(h).contains("invalid_token"));
        }
    }

    @Test
    void internalDataNeedsAServiceToken() throws Exception {
        TestUser student = student();

        assertThat(get("/internal/directory/users/" + student.id(), null).statusCode())
                .isEqualTo(401);
        // A signed-in browser's cookie is no credential here.
        HttpResponse<String> withCookie =
                http.send(
                        HttpRequest.newBuilder(url("/internal/directory/users/" + student.id()))
                                .header(
                                        "Cookie",
                                        "mdl_session=" + student.api().cookie("mdl_session"))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(withCookie.statusCode()).isEqualTo(401);

        HttpResponse<String> user =
                get("/internal/directory/users/" + student.id(), serviceToken());
        assertThat(user.statusCode()).isEqualTo(200);
        assertThat(ApiClient.JSON.readTree(user.body()).get("email").asText())
                .isEqualTo(student.email());
    }

    @Test
    void theDirectoryAnswersOtherServices() throws Exception {
        TestUser admin = admin();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        String token = serviceToken();

        JsonNode user =
                ApiClient.JSON.readTree(
                        get("/internal/directory/users/" + student.id(), token).body());
        assertThat(user.get("classId").asLong()).isEqualTo(classId);
        assertThat(user.get("roles").toString()).contains("STUDENT");

        JsonNode inClass =
                ApiClient.JSON.readTree(
                        post("/internal/directory/users/in-classes", List.of(classId), token)
                                .body());
        assertThat(inClass).hasSize(1);
        assertThat(inClass.get(0).get("id").asLong()).isEqualTo(student.id());

        // The owner's profile page also shows whether 2FA is on.
        JsonNode account =
                ApiClient.JSON.readTree(
                        get("/internal/directory/users/" + student.id() + "/account", token)
                                .body());
        assertThat(account.get("user").get("email").asText()).isEqualTo(student.email());
        assertThat(account.get("twoFaEnabled").asBoolean()).isFalse();

        assertThat(get("/internal/directory/users/999999999", token).statusCode()).isEqualTo(404);
    }

    @Test
    void theGatewayExchangesASessionCookieForAShortUserToken() throws Exception {
        TestUser student = student();
        String session = student.api().cookie("mdl_session");

        HttpResponse<String> resolved =
                post(
                        "/internal/sessions/resolve",
                        Map.of("token", session, "userAgent", "moodle-it/1.0"),
                        serviceToken());
        assertThat(resolved.statusCode()).isEqualTo(200);
        JsonNode body = ApiClient.JSON.readTree(resolved.body());
        assertThat(body.get("userId").asLong()).isEqualTo(student.id());
        assertThat(body.get("roles").toString()).contains("STUDENT");

        SignedJWT jwt = SignedJWT.parse(body.get("accessToken").asText());
        assertThat(jwt.verify(new RSASSAVerifier(jwks().getKeys().get(0).toRSAKey()))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo(String.valueOf(student.id()));
        assertThat(jwt.getJWTClaimsSet().getStringClaim("email")).isEqualTo(student.email());
        assertThat(jwt.getJWTClaimsSet().getStringClaim("sid"))
                .isEqualTo(body.get("sessionHash").asText());

        // A user token is not a service token: it opens nothing internal.
        assertThat(get("/internal/directory/users/" + student.id(), jwt.serialize()).statusCode())
                .isEqualTo(403);

        // Same browser checks as everywhere: another User-Agent is refused (and revokes it). Not a
        // 401, which here only ever means the caller's token was refused.
        HttpResponse<String> stolen =
                post(
                        "/internal/sessions/resolve",
                        Map.of("token", session, "userAgent", "evil/1.0"),
                        serviceToken());
        assertThat(stolen.statusCode()).isEqualTo(404);
        assertThat(
                        post(
                                        "/internal/sessions/resolve",
                                        Map.of("token", session, "userAgent", "moodle-it/1.0"),
                                        serviceToken())
                                .statusCode())
                .isEqualTo(404);
        student.api().get("/api/auth/session").expect(401);
    }

    private JWKSet jwks() throws Exception {
        HttpResponse<String> response = get("/internal/.well-known/jwks.json", null);
        assertThat(response.statusCode()).isEqualTo(200);
        JWKSet set = JWKSet.parse(response.body());
        assertThat(set.getKeys()).hasSize(1);
        assertThat(set.getKeys().get(0)).isInstanceOf(RSAKey.class);
        assertThat(set.getKeys().get(0).isPrivate()).isFalse();
        return set;
    }

    private String serviceToken() throws Exception {
        return ApiClient.JSON
                .readTree(token("it-service", "it-service-secret").body())
                .get("access_token")
                .asText();
    }

    private HttpResponse<String> token(String client, String secret) throws Exception {
        String basic =
                Base64.getEncoder()
                        .encodeToString((client + ":" + secret).getBytes(StandardCharsets.UTF_8));
        return http.send(
                HttpRequest.newBuilder(url("/internal/oauth2/token"))
                        .header("Authorization", "Basic " + basic)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String bearer) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(url(path));
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, Object body, String bearer) throws Exception {
        return http.send(
                HttpRequest.newBuilder(url(path))
                        .header("Authorization", "Bearer " + bearer)
                        .header("Content-Type", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofByteArray(
                                        ApiClient.JSON.writeValueAsBytes(body)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
