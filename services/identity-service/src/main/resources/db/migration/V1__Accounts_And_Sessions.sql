-- Accounts, classes, sessions and profile pictures, moved out of core's database. Same columns as
-- core's tables (its V1, V9, V10) so rows can be copied over with their ids; the never-used
-- users.profile_picture_url (core V4) is left behind. Other services keep only user and class ids
-- and hear about deletions through events, so nothing outside this database points in here.

CREATE TABLE classes
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE users
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    class_id       BIGINT       NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    two_fa_secret  VARCHAR(255) DEFAULT NULL,
    two_fa_enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_users_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL
);

CREATE TABLE user_roles
(
    user_id BIGINT      NOT NULL,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- One row per signed-in browser. token_signature is the SHA-256 hex digest of the opaque cookie
-- value, looked up on every request; the session is bound to the browser that created it
-- (user_agent_hash) and has idle (last_active) and absolute (expires_at) expiry.
CREATE TABLE user_sessions
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    device_name     VARCHAR(255),
    ip_address      VARCHAR(50),
    token_signature VARCHAR(64)  NOT NULL,
    last_active     TIMESTAMP    NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent_hash VARCHAR(64)  NULL DEFAULT NULL,
    created_at      TIMESTAMP    NULL DEFAULT NULL,
    expires_at      TIMESTAMP    NULL DEFAULT NULL,
    CONSTRAINT uq_user_session_token UNIQUE (token_signature),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- token is the SHA-256 hex digest of the emailed link's secret.
CREATE TABLE password_reset_tokens
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT       NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    CONSTRAINT fk_reset_token_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- See core's V9: retired pictures are purged after the retention period; deleting an account
-- keeps the row (user_id NULL) so its file is still purged on schedule.
CREATE TABLE profile_pictures
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NULL,
    storage_key  VARCHAR(64) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    size_bytes   INT         NOT NULL,
    sha256       VARCHAR(64) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retired_at   TIMESTAMP   NULL,
    purge_after  TIMESTAMP   NULL,
    CONSTRAINT uq_profile_picture_key UNIQUE (storage_key),
    CONSTRAINT fk_profile_picture_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_profile_picture_current ON profile_pictures (user_id, retired_at);
CREATE INDEX idx_profile_picture_purge ON profile_pictures (purge_after);

-- Spring Modulith's event publication registry (the transactional outbox), as in core's V13.
CREATE TABLE EVENT_PUBLICATION
(
    ID               VARCHAR(36)   NOT NULL,
    LISTENER_ID      VARCHAR(512)  NOT NULL,
    EVENT_TYPE       VARCHAR(512)  NOT NULL,
    SERIALIZED_EVENT VARCHAR(4000) NOT NULL,
    PUBLICATION_DATE TIMESTAMP(6)  NOT NULL,
    COMPLETION_DATE  TIMESTAMP(6)  DEFAULT NULL NULL,
    PRIMARY KEY (ID),
    INDEX EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX (COMPLETION_DATE)
);
