package moodlev2.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * A black-box HTTP client that calls the service the way the gateway does: every request carries a
 * freshly minted user token (the gateway swaps the browser's cookie for one), or none at all for an
 * anonymous caller. Cookies and CSRF are the gateway's and identity-service's business now.
 */
public final class ApiClient {

    static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    private final HttpClient http =
            HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
    private final String baseUrl;
    private final Supplier<String> token;

    /**
     * @param token a bearer token per request, {@code null} for an anonymous caller
     */
    public ApiClient(String baseUrl, Supplier<String> token) {
        this.baseUrl = baseUrl;
        this.token = token;
    }

    public Response get(String path) {
        return send("GET", path, HttpRequest.BodyPublishers.noBody(), null);
    }

    public Response post(String path, Object body) {
        return sendJson("POST", path, body);
    }

    public Response put(String path, Object body) {
        return sendJson("PUT", path, body);
    }

    public Response patch(String path, Object body) {
        return sendJson("PATCH", path, body);
    }

    public Response delete(String path) {
        return send("DELETE", path, HttpRequest.BodyPublishers.noBody(), null);
    }

    /**
     * Posts {@code multipart/form-data}; values are Strings, or a {@link FilePart} / {@link
     * JsonPart}.
     */
    public Response multipart(String method, String path, Map<String, Object> parts) {
        String boundary = "----moodle-it-" + UUID.randomUUID();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Map.Entry<String, Object> e : parts.entrySet()) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                Object v = e.getValue();
                if (v instanceof FilePart f) {
                    out.write(
                            ("Content-Disposition: form-data; name=\""
                                            + e.getKey()
                                            + "\"; filename=\""
                                            + f.filename()
                                            + "\"\r\nContent-Type: "
                                            + f.contentType()
                                            + "\r\n\r\n")
                                    .getBytes(StandardCharsets.UTF_8));
                    out.write(f.content());
                } else if (v instanceof JsonPart j) {
                    out.write(
                            ("Content-Disposition: form-data; name=\""
                                            + e.getKey()
                                            + "\"\r\nContent-Type: application/json\r\n\r\n")
                                    .getBytes(StandardCharsets.UTF_8));
                    out.write(JSON.writeValueAsBytes(j.value()));
                } else {
                    out.write(
                            ("Content-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n")
                                    .getBytes(StandardCharsets.UTF_8));
                    out.write(String.valueOf(v).getBytes(StandardCharsets.UTF_8));
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return send(
                method,
                path,
                HttpRequest.BodyPublishers.ofByteArray(out.toByteArray()),
                "multipart/form-data; boundary=" + boundary);
    }

    private Response sendJson(String method, String path, Object body) {
        try {
            HttpRequest.BodyPublisher publisher =
                    body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofByteArray(JSON.writeValueAsBytes(body));
            return send(method, path, publisher, body == null ? null : "application/json");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Response send(
            String method, String path, HttpRequest.BodyPublisher body, String contentType) {
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "application/json")
                        .method(method, body);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token.get());
        }
        try {
            HttpResponse<String> response =
                    http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(method + " " + path, response.statusCode(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public record FilePart(String filename, String contentType, byte[] content) {}

    public record JsonPart(Object value) {}

    /** A response, with assertion helpers that print the body when the status is unexpected. */
    public record Response(String request, int status, String body) {

        public Response expect(int expected) {
            assertThat(status).as("%s -> %s", request, body).isEqualTo(expected);
            return this;
        }

        public Response ok() {
            assertThat(status).as("%s -> %s", request, body).isBetween(200, 299);
            return this;
        }

        public JsonNode json() {
            try {
                return JSON.readTree(body);
            } catch (IOException e) {
                throw new IllegalStateException(request + " returned non-JSON: " + body, e);
            }
        }
    }
}
