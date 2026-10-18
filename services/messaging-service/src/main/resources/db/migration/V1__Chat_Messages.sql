-- Conversations, moved out of core's database. Same shape as core's chat_messages (V1, V8, V12),
-- so rows can be copied over unchanged. People are referred to by email.
CREATE TABLE IF NOT EXISTS chat_messages
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender     VARCHAR(255) NOT NULL,
    recipient  VARCHAR(255) DEFAULT NULL,
    content    TEXT,
    is_private BOOLEAN      DEFAULT FALSE,
    timestamp  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    read_at    DATETIME     NULL
);

CREATE INDEX idx_chat_sender_ts ON chat_messages (sender, timestamp);
CREATE INDEX idx_chat_recipient_ts ON chat_messages (recipient, timestamp);
CREATE INDEX idx_chat_messages_unread ON chat_messages (recipient, sender, read_at);
