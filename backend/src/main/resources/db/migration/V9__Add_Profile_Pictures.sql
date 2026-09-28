-- Profile pictures, one row per uploaded image. The file itself lives outside the database, in a
-- private directory, under a random server-chosen name (storage_key); nothing the uploader sends
-- ends up in a path.
--
-- Retention: a picture is "current" while retired_at is NULL. It is retired when it is replaced,
-- removed, or its account is deactivated or deleted; purge_after is then set to retirement plus
-- the retention period (90 days by default), and a daily job permanently deletes the file and the
-- row once that passes.
--
-- ON DELETE SET NULL rather than CASCADE: deleting an account must not orphan the file on disk
-- with no record of it. The row survives with user_id NULL, the job retires it, and it is purged
-- on the same schedule as any other retired picture.
--
-- users.profile_picture_url (V4) was never used by the application and is left untouched.

CREATE TABLE profile_pictures (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NULL,
    storage_key  VARCHAR(64)  NOT NULL,
    content_type VARCHAR(50)  NOT NULL,
    size_bytes   INT          NOT NULL,
    sha256       VARCHAR(64)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retired_at   TIMESTAMP    NULL,
    purge_after  TIMESTAMP    NULL,

    CONSTRAINT uq_profile_picture_key UNIQUE (storage_key),
    CONSTRAINT fk_profile_picture_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_profile_picture_current ON profile_pictures (user_id, retired_at);
CREATE INDEX idx_profile_picture_purge ON profile_pictures (purge_after);
