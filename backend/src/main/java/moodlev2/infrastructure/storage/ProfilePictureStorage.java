package moodlev2.infrastructure.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Private on-disk store for profile pictures.
 *
 * <p>Deliberately separate from {@code uploads/}, which holds course resources that may be served
 * publicly. Files here are only ever returned through the authenticated API. Names are generated
 * server-side and every key is checked against a strict pattern before it touches a path, so
 * nothing an uploader sends can influence where a file is written or read from.
 */
@Component
public class ProfilePictureStorage {

    private static final Logger log = LoggerFactory.getLogger(ProfilePictureStorage.class);

    private static final Pattern KEY =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");

    private static final String TEMP_PREFIX = ".upload-";

    private final Path root;

    public ProfilePictureStorage(
            @Value("${app.profile-pictures.dir:profile-pictures}") String directory) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
            restrict(root, "rwx------");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create profile picture directory " + root, e);
        }
    }

    public String newKey() {
        return UUID.randomUUID() + ".jpg";
    }

    /** Written to a temp file first and moved into place, so a reader never sees a partial file. */
    public void write(String key, byte[] bytes) {
        Path target = resolve(key);
        try {
            Path temp = Files.createTempFile(root, TEMP_PREFIX, ".tmp");
            restrict(temp, "rw-------");
            Files.write(temp, bytes);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the picture. Please try again.", e);
        }
    }

    public Optional<byte[]> read(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            log.warn("Could not read profile picture {}", key);
            return Optional.empty();
        }
    }

    /**
     * @return false only if the file exists and could not be removed (so the caller retries)
     */
    public boolean delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
            return true;
        } catch (IOException e) {
            log.warn("Could not delete profile picture {}", key);
            return false;
        }
    }

    /**
     * Every file in the directory, including leftover temp files from interrupted writes, for the
     * retention job's orphan sweep.
     */
    public List<StoredFile> list() {
        List<StoredFile> files = new ArrayList<>();
        try (DirectoryStream<Path> dir = Files.newDirectoryStream(root)) {
            for (Path p : dir) {
                Path fileName = p.getFileName();
                if (fileName == null || !Files.isRegularFile(p)) {
                    continue;
                }
                String name = fileName.toString();
                boolean isPicture = KEY.matcher(name).matches();
                if (isPicture || name.startsWith(TEMP_PREFIX)) {
                    files.add(
                            new StoredFile(
                                    name, isPicture, Files.getLastModifiedTime(p).toInstant()));
                }
            }
        } catch (IOException e) {
            log.warn("Could not list profile picture directory");
        }
        return files;
    }

    /** Removes a leftover temp file by name; only names this class created are accepted. */
    public void deleteTemp(String name) {
        if (!name.startsWith(TEMP_PREFIX) || name.contains("/") || name.contains("\\")) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(name));
        } catch (IOException e) {
            log.warn("Could not delete temp file {}", name);
        }
    }

    private Path resolve(String key) {
        if (key == null || !KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid picture key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid picture key");
        }
        return path;
    }

    /** Owner-only access where the filesystem supports POSIX permissions; a no-op elsewhere. */
    private static void restrict(Path path, String permissions) {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        } catch (UnsupportedOperationException | IOException e) {
            // Non-POSIX filesystem (e.g. Windows dev machines): rely on the directory's ACLs.
        }
    }

    /**
     * @param isPicture false for a leftover temp file
     */
    public record StoredFile(String name, boolean isPicture, Instant lastModified) {}
}
