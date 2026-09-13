-- Proctoring signals captured during a quiz attempt. The quiz page cannot see other
-- tabs or sites (browsers forbid it), so what it records is that the student left the
-- exam surface: tab hidden, window blurred, fullscreen exited, plus copy/paste. Each
-- row is one event, tied to the attempt, and is disclosed to the student on-screen.

CREATE TABLE proctor_events (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id  BIGINT NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    detail      VARCHAR(255) NULL,
    client_ts   BIGINT NULL,           -- client epoch millis, for ordering within a burst
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_proctor_attempt FOREIGN KEY (attempt_id)
        REFERENCES quiz_attempts (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci;

CREATE INDEX idx_proctor_attempt ON proctor_events (attempt_id, occurred_at);
