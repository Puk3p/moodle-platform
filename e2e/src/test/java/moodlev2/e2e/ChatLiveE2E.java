package moodlev2.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Live delivery: a student's socket, opened through the gateway with only the session cookie,
 * receives a teacher's message the moment it is sent, and is closed when the student signs out.
 */
class ChatLiveE2E extends E2EBase {

    @Test
    void aMessageArrivesOnTheRecipientsSocketAndLogoutClosesIt() throws Exception {
        TestUser admin = admin();
        TestUser teacher = teacher();
        TestUser student = student();
        long classId = createClass(admin);
        placeInClass(admin, student, classId);
        createCourse(teacher, classId);

        StompSocket socket = StompSocket.open(student);
        socket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\n\n\0");
        assertThat(socket.next()).startsWith("CONNECTED");
        socket.send("SUBSCRIBE\nid:sub-0\ndestination:/user/queue/private\n\n\0");

        String content = "Live " + System.nanoTime();
        teacher.api()
                .post(
                        "/api/chat/messages",
                        Map.of("recipient", student.email(), "content", content))
                .expect(201);

        String frame = socket.next();
        assertThat(frame).startsWith("MESSAGE").contains(content);

        // Signing out ends the session; core's event makes messaging close the socket.
        student.api().post("/api/auth/logout", null).expect(204);
        assertThat(socket.awaitClose()).isTrue();
    }

    /** Just enough STOMP over a raw WebSocket (what the SPA's client does on /ws/websocket). */
    static final class StompSocket implements WebSocket.Listener {
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final StringBuilder partial = new StringBuilder();
        private final java.util.concurrent.CountDownLatch closed =
                new java.util.concurrent.CountDownLatch(1);
        private WebSocket ws;

        static StompSocket open(TestUser user) throws Exception {
            StompSocket s = new StompSocket();
            String ws = BASE_URL.replaceFirst("^http", "ws") + "/ws/websocket";
            s.ws =
                    HttpClient.newHttpClient()
                            .newWebSocketBuilder()
                            .connectTimeout(Duration.ofSeconds(10))
                            .header("Cookie", "mdl_session=" + user.api().cookie("mdl_session"))
                            .header("User-Agent", "moodle-it/1.0")
                            .header("Origin", "http://localhost:4200")
                            .buildAsync(URI.create(ws), s)
                            .get(15, TimeUnit.SECONDS);
            return s;
        }

        void send(String frame) {
            ws.sendText(frame, true).join();
        }

        String next() throws InterruptedException {
            String frame = frames.poll(15, TimeUnit.SECONDS);
            assertThat(frame).as("STOMP frame").isNotNull();
            return frame;
        }

        boolean awaitClose() throws InterruptedException {
            return closed.await(20, TimeUnit.SECONDS);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                String text = partial.toString();
                partial.setLength(0);
                if (!text.isBlank()) {
                    frames.add(text);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.countDown();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.countDown();
        }
    }
}
