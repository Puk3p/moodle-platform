package moodlev2.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class ClientAddressTest {

    /** The default from application.yml: loopback and the private networks. */
    private final ClientAddress clientAddress =
            new ClientAddress(
                    "127\\.0\\.0\\.1|0:0:0:0:0:0:0:1|::1|10\\..*|192\\.168\\..*"
                            + "|172\\.(1[6-9]|2[0-9]|3[01])\\..*");

    private static MockServerHttpRequest from(String peer, String... forwardedFor) {
        return MockServerHttpRequest.get("/api/courses")
                .remoteAddress(new InetSocketAddress(peer, 40000))
                .header(ClientAddress.X_FORWARDED_FOR, forwardedFor)
                .build();
    }

    @Test
    void behindNginxAndDockerTheClientIsTheAddressNginxReported() {
        // nginx on the host appends the client; Docker's port forwarding is the gateway's peer.
        MockServerHttpRequest request = from("172.18.0.1", "203.0.113.7");

        assertThat(clientAddress.of(request)).isEqualTo("203.0.113.7");
        assertThat(clientAddress.chain(request)).containsExactly("203.0.113.7", "172.18.0.1");
    }

    @Test
    void anAddressAClientMadeUpIsNotTakenOverTheOneNginxAppended() {
        MockServerHttpRequest request = from("172.18.0.1", "198.51.100.1, 203.0.113.7");

        assertThat(clientAddress.of(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void aPeerThatIsNoProxyIsTheClientWhateverItClaims() {
        MockServerHttpRequest request = from("203.0.113.7", "198.51.100.1");

        assertThat(clientAddress.of(request)).isEqualTo("203.0.113.7");
        assertThat(clientAddress.chain(request)).containsExactly("203.0.113.7");
    }

    @Test
    void withNothingButProxiesTheFurthestOneIsTheClient() {
        assertThat(clientAddress.of(from("127.0.0.1"))).isEqualTo("127.0.0.1");
        assertThat(clientAddress.of(from("172.18.0.1", "10.0.0.5"))).isEqualTo("10.0.0.5");
    }

    @Test
    void anUnknownPeerHasNoAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/courses").build();

        assertThat(clientAddress.of(request)).isNull();
        assertThat(clientAddress.chain(request)).isEmpty();
    }
}
