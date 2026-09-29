package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One signed-in browser. The browser holds a random token in an HttpOnly cookie; only its SHA-256
 * hash is stored here, so a copy of this table cannot be used to sign in.
 */
@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UserSessionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "device_name")
    private String deviceName;

    /** Most recent client address seen for this session. */
    @Column(name = "ip_address")
    private String ipAddress;

    /** SHA-256 of the cookie token. */
    @Column(name = "token_signature", nullable = false, unique = true)
    private String tokenSignature;

    /**
     * SHA-256 of the User-Agent that signed in. A cookie replayed from a different browser does not
     * match and the session is revoked on the spot.
     */
    @Column(name = "user_agent_hash")
    private String userAgentHash;

    @Column(name = "created_at")
    private Instant createdAt;

    /** Absolute end of the session, however active it is. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Idle expiry is measured from here. */
    @Column(name = "last_active")
    private Instant lastActive;
}
