package moodlev2.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "token_signature")
    private String tokenSignature;

    @Column(name = "last_active")
    private Instant lastActive;

    @PrePersist
    protected void onCreate() {
        this.lastActive = Instant.now();
    }
}
