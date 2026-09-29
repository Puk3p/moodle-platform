package moodlev2.application.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileStorageServiceTest {

    @TempDir Path dir;

    private final FileStorageService storage = new FileStorageService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(storage, "fileStorageLocation", dir.toAbsolutePath());
    }

    private long filesOnDisk() throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.count();
        }
    }

    @Test
    void storedFileCanBeDeletedByItsUrl() throws IOException {
        String url =
                storage.storeFile(
                        new MockMultipartFile(
                                "file", "notes.pdf", "application/pdf", new byte[] {1}));

        assertThat(url).startsWith("/uploads/").endsWith("_notes.pdf");
        assertThat(filesOnDisk()).isEqualTo(1);

        storage.deleteFile(url);
        assertThat(filesOnDisk()).isZero();
    }

    @Test
    void anInterruptedUploadLeavesNoPartialFileBehind() throws IOException {
        MockMultipartFile dying =
                new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[] {1}) {
                    @Override
                    public InputStream getInputStream() {
                        return new ConnectionResetStream();
                    }
                };

        assertThatThrownBy(() -> storage.storeFile(dying))
                .isInstanceOf(IllegalStateException.class);
        assertThat(filesOnDisk()).isZero();
    }

    @Test
    void disallowedTypeIsRejectedBeforeWriting() throws IOException {
        assertThatThrownBy(
                        () ->
                                storage.storeFile(
                                        new MockMultipartFile(
                                                "file", "page.html", "text/html", new byte[] {1})))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(filesOnDisk()).isZero();
    }

    /** Delivers a few bytes, then fails like a dropped connection. */
    private static final class ConnectionResetStream extends InputStream {
        private boolean served;

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            return read(one, 0, 1) < 0 ? -1 : one[0];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (served) {
                throw new IOException("connection reset");
            }
            served = true;
            int n = Math.min(len, 16);
            java.util.Arrays.fill(b, off, off + n, (byte) 7);
            return n;
        }
    }
}
