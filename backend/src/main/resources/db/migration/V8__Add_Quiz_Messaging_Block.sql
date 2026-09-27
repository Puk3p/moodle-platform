-- Per-quiz switch. While a student has a live attempt on a quiz with this set, messaging is
-- unavailable to them: the UI hides it and every chat endpoint refuses the call, so it cannot be
-- reached by calling the API directly either. Off for existing quizzes; teachers opt in.
ALTER TABLE quizzes
    ADD COLUMN block_messaging BOOLEAN NOT NULL DEFAULT FALSE;

-- History is read per participant (sender = me OR recipient = me). Without these every chat
-- load is a full scan of the message table.
CREATE INDEX idx_chat_sender_ts ON chat_messages (sender, timestamp);
CREATE INDEX idx_chat_recipient_ts ON chat_messages (recipient, timestamp);
