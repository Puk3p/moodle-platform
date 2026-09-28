package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One uploaded profile picture. Current while {@code retiredAt} is null; once retired it is kept
 * until {@code purgeAfter}, then the retention job deletes the file and this row.
 */
@Entity
@Table(name = "profile_pictures")
@Getter
@Setter
@NoArgsConstructor
public class ProfilePictureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null once the owning account has been deleted. */
    @Column(name = "user_id")
    private Long userId;

    /** Server-generated file name; never derived from anything the uploader sent. */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    /** Of the stored (re-encoded) file, for incident response and integrity checks. */
    @Column(nullable = false)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    @Column(name = "purge_after")
    private Instant purgeAfter;
}
