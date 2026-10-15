package moodlev2.identity.it;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A black-box HTTP client that behaves like the SPA: it keeps the cookies the server sets, echoes
 * the readable XSRF cookie in {@code X-XSRF-TOKEN} on every state-changing call, and presents a
 * stable User-Agent (the session is bound to it).
 *
 * <p>It only talks HTTP to a base URL, so the same flows can later run against the API gateway in
 * front of the extracted services instead of the monolith.
 *
 * <p>Every client gets its own documentation-range address in {@code X-Forwarded-For}. The server
 * trusts that header from loopback (it normally sits behind nginx), so each test user has its own
 * bucket in the per-address sign-in rate limiter instead of all of them sharing 127.0.0.1.
 */
public final class ApiClient {

    static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final AtomicInteger NEXT_ADDRESS = new AtomicInteger(1);
    private static final String USER_AGENT = "moodle-it/1.0";

    private final HttpClient http =
            HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
    private final String baseUrl;
    private final String clientAddress;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        int n = NEXT_ADDRESS.getAndIncrement();
        this.clientAddress =
                (n <= 250 ? "198.51.100." : "203.0.113.") + (n <= 250 ? n : (n - 250) % 250 + 1);
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

    public boolean hasCookie(String name) {
        return cookies.containsKey(name);
    }

    public String cookie(String name) {
        return cookies.get(name);
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
        boolean mutating = !"GET".equals(method);
        if (mutating && csrfToken() == null) {
            // The server writes the XSRF cookie on any response; fetch one like the SPA does at
            // start-up.
            get("/api/auth/session");
        }
        HttpRequest.Builder request =
                HttpRequest.newBuilder(URI.create(baseUrl + path))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", USER_AGENT)
                        .header("X-Forwarded-For", clientAddress)
                        .header("Accept", "application/json")
                        .method(method, body);
        if (contentType != null) {
            request.header("Content-Type", contentType);
        }
        if (mutating && csrfToken() != null) {
            request.header("X-XSRF-TOKEN", csrfToken());
        }
        if (!cookies.isEmpty()) {
            StringBuilder header = new StringBuilder();
            cookies.forEach(
                    (k, v) -> {
                        if (!header.isEmpty()) {
                            header.append("; ");
                        }
                        header.append(k).append('=').append(v);
                    });
            request.header("Cookie", header.toString());
        }
        try {
            HttpResponse<String> response =
                    http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            storeCookies(response.headers().allValues("Set-Cookie"));
            return new Response(method + " " + path, response.statusCode(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String csrfToken() {
        String token = cookies.get("XSRF-TOKEN");
        return token != null ? token : cookies.get("__Host-XSRF-TOKEN");
    }

    private void storeCookies(List<String> headers) {
        for (String header : headers) {
            String[] attributes = header.split(";");
            String pair = attributes[0].trim();
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String name = pair.substring(0, eq);
            String value = pair.substring(eq + 1);
            boolean expired = value.isEmpty();
            for (int i = 1; i < attributes.length; i++) {
                String a = attributes[i].trim().toLowerCase();
                if (a.equals("max-age=0") || a.startsWith("expires=thu, 01 jan 1970")) {
                    expired = true;
                }
            }
            if (expired) {
                cookies.remove(name);
            } else {
                cookies.put(name, value);
            }
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
