-- Per-student extra quiz attempts granted by a teacher, on top of quizzes.max_attempts.
-- Reconstructed from the live schema on sentinel-vps (applied there 2026-03-28).

CREATE TABLE quiz_extra_attempts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id        BIGINT NOT NULL,
    user_id        BIGINT NOT NULL,
    extra_attempts INT NOT NULL DEFAULT 0,
    granted_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_quiz_user UNIQUE (quiz_id, user_id),
    CONSTRAINT fk_qea_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE,
    CONSTRAINT fk_qea_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;
