package moodlev2.application.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import moodlev2.domain.user.Role;
import moodlev2.infrastructure.persistence.jpa.ProfilePictureRepository;
import moodlev2.infrastructure.persistence.jpa.SpringDataUserRepository;
import moodlev2.infrastructure.persistence.jpa.entity.ProfilePictureEntity;
import moodlev2.infrastructure.persistence.jpa.entity.UserEntity;
import moodlev2.infrastructure.storage.ProfilePictureStorage;
import moodlev2.infrastructure.storage.ProfilePictureStorage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Profile pictures for students and teachers, with a retention limit.
 *
 * <p>Retention: a picture is kept while it is the user's current one. Once it stops being current
 * (replaced, removed, or its account deactivated or deleted) it is retained for the configured
 * period, 90 days by default, and then permanently deleted by {@link #enforceRetention()}.
 */
@Service
public class ProfilePictureService {

    private static final Logger log = LoggerFactory.getLogger(ProfilePictureService.class);

    /** Changes per user per rolling hour; stops a script churning storage or the retention log. */
    static final int MAX_CHANGES_PER_HOUR = 10;

    /** Files on disk with no database row are left alone this long, then treated as debris. */
    static final Duration ORPHAN_GRACE = Duration.ofDays(1);

    private final ProfilePictureRepository pictures;
    private final SpringDataUserRepository users;
    private final ProfilePictureStorage storage;
    private final Duration retention;
    private final Clock clock;

    private final Map<Long, Deque<Instant>> recentChanges = new ConcurrentHashMap<>();

    @Autowired
    public ProfilePictureService(
            ProfilePictureRepository pictures,
            SpringDataUserRepository users,
            ProfilePictureStorage storage,
            @Value("${app.profile-pictures.retention-days:90}") long retentionDays) {
        this(pictures, users, storage, Duration.ofDays(retentionDays), Clock.systemUTC());
    }

    ProfilePictureService(
            ProfilePictureRepository pictures,
            SpringDataUserRepository users,
            ProfilePictureStorage storage,
            Duration retention,
            Clock clock) {
        this.pictures = pictures;
        this.users = users;
        this.storage = storage;
        this.retention = retention;
        this.clock = clock;
    }

    public record Picture(byte[] bytes, String contentType, Instant updatedAt) {}

    public record RetentionReport(int retired, int purged, int orphansDeleted) {}

    @Transactional(readOnly = true)
    public Optional<Picture> current(String email) {
        UserEntity user = requireEligible(email);
        return pictures.findFirstByUserIdAndRetiredAtIsNull(user.getId())
                .flatMap(
                        p ->
                                storage.read(p.getStorageKey())
                                        .map(
                                                bytes ->
                                                        new Picture(
                                                                bytes,
                                                                p.getContentType(),
                                                                p.getCreatedAt())));
    }

    /**
     * Validates, cleans and stores a new picture, and retires the previous one. The file is written
     * before the database rows; if the database step fails the file is removed straight away, and
     * anything left by a failure after that is caught by the orphan sweep.
     */
    @Transactional
    public Instant replace(String email, MultipartFile file) {
        UserEntity user = requireEligible(email);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image to upload.");
        }
        if (file.getSize() > ProfilePictureImages.MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "The image is larger than 2 MB.");
        }
        acquireChangeSlot(user.getId());

        byte[] input;
        try {
            input = file.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("The upload could not be read. Please try again.");
        }
        ProfilePictureImages.Processed clean = ProfilePictureImages.process(input);

        Instant now = clock.instant();
        String key = storage.newKey();
        storage.write(key, clean.jpeg());
        try {
            pictures.findFirstByUserIdAndRetiredAtIsNull(user.getId())
                    .ifPresent(old -> retire(old, now));

            ProfilePictureEntity row = new ProfilePictureEntity();
            row.setUserId(user.getId());
            row.setStorageKey(key);
            row.setContentType(ProfilePictureImages.OUTPUT_CONTENT_TYPE);
            row.setSizeBytes(clean.jpeg().length);
            row.setSha256(sha256(clean.jpeg()));
            row.setCreatedAt(now);
            pictures.save(row);
        } catch (RuntimeException e) {
            storage.delete(key);
            throw e;
        }

        log.info(
                "Profile picture updated: user={} bytesIn={} bytesStored={} side={}",
                user.getId(),
                input.length,
                clean.jpeg().length,
                clean.side());
        return now;
    }

    /** Retires the current picture; it is kept for the retention period, then deleted. */
    @Transactional
    public void remove(String email) {
        UserEntity user = requireEligible(email);
        Instant now = clock.instant();
        pictures.findFirstByUserIdAndRetiredAtIsNull(user.getId())
                .ifPresent(
                        p -> {
                            retire(p, now);
                            log.info("Profile picture removed: user={}", user.getId());
                        });
    }

    /**
     * The retention pass, run daily by {@code ProfilePictureRetentionJob}:
     *
     * <ol>
     *   <li>retire current pictures whose account was deleted or deactivated, starting their clock;
     *   <li>permanently delete every retired picture past its purge date, file first;
     *   <li>delete files on disk that no row refers to (interrupted uploads), after a grace period.
     * </ol>
     */
    @Transactional
    public RetentionReport enforceRetention() {
        Instant now = clock.instant();

        int retired = 0;
        for (ProfilePictureEntity p : pictures.findByUserIdIsNullAndRetiredAtIsNull()) {
            retire(p, now);
            retired++;
        }
        for (ProfilePictureEntity p : pictures.findCurrentOfInactiveUsers()) {
            retire(p, now);
            retired++;
        }

        int purged = 0;
        for (ProfilePictureEntity p : pictures.findByPurgeAfterBefore(now)) {
            // Row goes only once the file is gone, so a failed delete is retried tomorrow.
            if (storage.delete(p.getStorageKey())) {
                pictures.delete(p);
                purged++;
            }
        }

        int orphans = 0;
        Instant orphanCutoff = now.minus(ORPHAN_GRACE);
        List<StoredFile> files = storage.list();
        for (StoredFile f : files) {
            if (!f.lastModified().isBefore(orphanCutoff)) {
                continue;
            }
            if (!f.isPicture()) {
                storage.deleteTemp(f.name());
                orphans++;
            } else if (!pictures.existsByStorageKey(f.name())) {
                storage.delete(f.name());
                orphans++;
            }
        }

        if (retired + purged + orphans > 0) {
            log.info(
                    "Profile picture retention: retired={} purged={} orphansDeleted={}",
                    retired,
                    purged,
                    orphans);
        }
        return new RetentionReport(retired, purged, orphans);
    }

    private void retire(ProfilePictureEntity p, Instant now) {
        p.setRetiredAt(now);
        p.setPurgeAfter(now.plus(retention));
        pictures.save(p);
    }

    /** Pictures are for students and teachers; admin-only accounts have none. */
    private UserEntity requireEligible(String email) {
        UserEntity user =
                users.findByEmail(email)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.FORBIDDEN, "Not allowed."));
        boolean eligible =
                user.isActive()
                        && (user.getRoles().contains(Role.STUDENT)
                                || user.getRoles().contains(Role.TEACHER));
        if (!eligible) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Profile pictures are available to students and teachers.");
        }
        return user;
    }

    private void acquireChangeSlot(Long userId) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(Duration.ofHours(1));
        Deque<Instant> times = recentChanges.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(windowStart)) {
                times.pollFirst();
            }
            if (times.size() >= MAX_CHANGES_PER_HOUR) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too many picture changes. Try again in an hour.");
            }
            times.addLast(now);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
