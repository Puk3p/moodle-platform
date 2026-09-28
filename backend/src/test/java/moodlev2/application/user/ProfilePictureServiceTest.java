package moodlev2.application.user;

import static moodlev2.application.user.PictureTestData.png;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ProfilePictureRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProfilePictureEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.infrastructure.storage.ProfilePictureStorage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProfilePictureServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-28T12:00:00Z");
    private static final Duration RETENTION = Duration.ofDays(90);

    @Mock private ProfilePictureRepository pictures;
    @Mock private SpringDataUserRepository users;
    @TempDir Path dir;

    private ProfilePictureStorage storage;
    private ProfilePictureService service;

    private final UserEntity student = user(1, "student@test.com", Role.STUDENT);
    private final UserEntity teacher = user(2, "teacher@test.com", Role.TEACHER);
    private final UserEntity admin = user(3, "admin@test.com", Role.ADMIN);

    /** Rows "saved" through the mocked repository. */
    private final List<ProfilePictureEntity> saved = new ArrayList<>();

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    void setUp() {
        storage = new ProfilePictureStorage(dir.toString());
        ReflectionTestUtils.invokeMethod(storage, "init");
        service =
                new ProfilePictureService(
                        pictures, users, storage, RETENTION, Clock.fixed(NOW, ZoneOffset.UTC));

        for (UserEntity u : List.of(student, teacher, admin)) {
            lenient().when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        }
        lenient()
                .when(pictures.save(any(ProfilePictureEntity.class)))
                .thenAnswer(
                        inv -> {
                            ProfilePictureEntity p = inv.getArgument(0);
                            if (!saved.contains(p)) {
                                saved.add(p);
                            }
                            return p;
                        });
    }

    private static UserEntity user(long id, String email, Role... roles) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail(email);
        u.setRoles(new HashSet<>(List.of(roles)));
        u.setActive(true);
        return u;
    }

    private static MockMultipartFile upload(byte[] bytes) {
        // Declared type and name are whatever a client claims; the service must not care.
        return new MockMultipartFile("file", "me.png", "image/png", bytes);
    }

    private static HttpStatus statusOf(Throwable t) {
        return HttpStatus.valueOf(((ResponseStatusException) t).getStatusCode().value());
    }

    private List<String> filesOnDisk() throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).toList();
        }
    }

    private ProfilePictureEntity current(Long userId, String key) {
        ProfilePictureEntity p = new ProfilePictureEntity();
        p.setUserId(userId);
        p.setStorageKey(key);
        p.setContentType("image/jpeg");
        p.setCreatedAt(NOW.minus(Duration.ofDays(10)));
        return p;
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @Test
    void studentsAndTeachersCanUpload() throws IOException {
        service.replace(student.getEmail(), upload(png(300, 300)));
        service.replace(teacher.getEmail(), upload(png(300, 300)));

        assertThat(saved).hasSize(2);
        assertThat(filesOnDisk()).hasSize(2);
        assertThat(saved.get(0).getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.get(0).getSha256()).hasSize(64);
        assertThat(saved.get(0).getRetiredAt()).isNull();
    }

    @Test
    void storedNameIsServerGeneratedNotTheUploadersFilename() throws IOException {
        MockMultipartFile hostile =
                new MockMultipartFile("file", "../../etc/passwd.png", "image/png", png(100, 100));

        service.replace(student.getEmail(), hostile);

        assertThat(filesOnDisk()).singleElement().asString().matches("[0-9a-f-]{36}\\.jpg");
    }

    @Test
    void replacingRetiresThePreviousPictureForTheRetentionPeriod() {
        ProfilePictureEntity old = current(1L, "11111111-1111-1111-1111-111111111111.jpg");
        when(pictures.findFirstByUserIdAndRetiredAtIsNull(1L)).thenReturn(Optional.of(old));

        service.replace(student.getEmail(), upload(png(200, 200)));

        assertThat(old.getRetiredAt()).isEqualTo(NOW);
        assertThat(old.getPurgeAfter()).isEqualTo(NOW.plus(Duration.ofDays(90)));
    }

    @Test
    void adminOnlyAndDeactivatedAccountsCannotUpload() {
        UserEntity gone = user(4, "gone@test.com", Role.STUDENT);
        gone.setActive(false);
        when(users.findByEmail(gone.getEmail())).thenReturn(Optional.of(gone));

        for (String email : List.of(admin.getEmail(), gone.getEmail())) {
            assertThatThrownBy(() -> service.replace(email, upload(png(100, 100))))
                    .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.FORBIDDEN));
        }
        verify(pictures, never()).save(any());
    }

    @Test
    void oversizedUploadIsRefusedBeforeDecoding() {
        byte[] big = new byte[ProfilePictureImages.MAX_UPLOAD_BYTES + 1];

        assertThatThrownBy(() -> service.replace(student.getEmail(), upload(big)))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void emptyUploadIsRefused() {
        assertThatThrownBy(() -> service.replace(student.getEmail(), upload(new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changesAreRateLimitedPerUser() {
        for (int i = 0; i < ProfilePictureService.MAX_CHANGES_PER_HOUR; i++) {
            service.replace(student.getEmail(), upload(png(64, 64)));
        }

        assertThatThrownBy(() -> service.replace(student.getEmail(), upload(png(64, 64))))
                .satisfies(t -> assertThat(statusOf(t)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS));
        // Another user is unaffected.
        service.replace(teacher.getEmail(), upload(png(64, 64)));
    }

    @Test
    void aFailedDatabaseWriteLeavesNoFileBehind() throws IOException {
        doThrow(new IllegalStateException("db down")).when(pictures).save(any());

        assertThatThrownBy(() -> service.replace(student.getEmail(), upload(png(100, 100))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(filesOnDisk()).isEmpty();
    }

    @Test
    void currentReturnsTheStoredBytes() {
        service.replace(student.getEmail(), upload(png(100, 100)));
        ProfilePictureEntity row = saved.get(0);
        when(pictures.findFirstByUserIdAndRetiredAtIsNull(1L)).thenReturn(Optional.of(row));

        ProfilePictureService.Picture pic = service.current(student.getEmail()).orElseThrow();

        assertThat(pic.contentType()).isEqualTo("image/jpeg");
        assertThat(ProfilePictureImages.sniffFormat(pic.bytes())).isEqualTo("jpeg");
    }

    @Test
    void removeRetiresTheCurrentPicture() {
        ProfilePictureEntity cur = current(1L, "22222222-2222-2222-2222-222222222222.jpg");
        when(pictures.findFirstByUserIdAndRetiredAtIsNull(1L)).thenReturn(Optional.of(cur));

        service.remove(student.getEmail());

        assertThat(cur.getRetiredAt()).isEqualTo(NOW);
        assertThat(cur.getPurgeAfter()).isEqualTo(NOW.plus(RETENTION));
    }

    // ── Retention ────────────────────────────────────────────────────────────

    @Test
    void picturesPastTheirPurgeDateAreDeletedFileAndRow() throws IOException {
        String key = storage.newKey();
        storage.write(key, new byte[] {1, 2, 3});
        ProfilePictureEntity expired = current(1L, key);
        expired.setRetiredAt(NOW.minus(Duration.ofDays(91)));
        expired.setPurgeAfter(NOW.minus(Duration.ofDays(1)));
        when(pictures.findByPurgeAfterBefore(NOW)).thenReturn(List.of(expired));
        lenient().when(pictures.existsByStorageKey(anyString())).thenReturn(true);

        ProfilePictureService.RetentionReport report = service.enforceRetention();

        assertThat(report.purged()).isEqualTo(1);
        assertThat(filesOnDisk()).isEmpty();
        verify(pictures).delete(expired);
    }

    @Test
    void deletedAndDeactivatedAccountsStartTheRetentionClock() {
        ProfilePictureEntity ofDeleted = current(null, "33333333-3333-3333-3333-333333333333.jpg");
        ProfilePictureEntity ofInactive = current(5L, "44444444-4444-4444-4444-444444444444.jpg");
        when(pictures.findByUserIdIsNullAndRetiredAtIsNull()).thenReturn(List.of(ofDeleted));
        when(pictures.findCurrentOfInactiveUsers()).thenReturn(List.of(ofInactive));

        ProfilePictureService.RetentionReport report = service.enforceRetention();

        assertThat(report.retired()).isEqualTo(2);
        for (ProfilePictureEntity p : List.of(ofDeleted, ofInactive)) {
            assertThat(p.getRetiredAt()).isEqualTo(NOW);
            assertThat(p.getPurgeAfter()).isEqualTo(NOW.plus(RETENTION));
        }
    }

    @Test
    void orphanFilesAreSweptOnlyAfterTheGracePeriod() throws IOException {
        String oldOrphan = storage.newKey();
        String freshOrphan = storage.newKey();
        storage.write(oldOrphan, new byte[] {1});
        storage.write(freshOrphan, new byte[] {1});
        Files.setLastModifiedTime(
                dir.resolve(oldOrphan), FileTime.from(NOW.minus(Duration.ofDays(2))));
        Files.setLastModifiedTime(dir.resolve(freshOrphan), FileTime.from(NOW));
        Path staleTemp = Files.createFile(dir.resolve(".upload-123.tmp"));
        Files.setLastModifiedTime(staleTemp, FileTime.from(NOW.minus(Duration.ofDays(2))));
        when(pictures.existsByStorageKey(anyString())).thenReturn(false);

        ProfilePictureService.RetentionReport report = service.enforceRetention();

        assertThat(report.orphansDeleted()).isEqualTo(2);
        assertThat(filesOnDisk()).containsExactly(freshOrphan);
    }

    @Test
    void referencedFilesAreNeverSwept() throws IOException {
        String key = storage.newKey();
        storage.write(key, new byte[] {1});
        Files.setLastModifiedTime(dir.resolve(key), FileTime.from(NOW.minus(Duration.ofDays(30))));
        when(pictures.existsByStorageKey(key)).thenReturn(true);

        service.enforceRetention();

        assertThat(filesOnDisk()).containsExactly(key);
    }
}
