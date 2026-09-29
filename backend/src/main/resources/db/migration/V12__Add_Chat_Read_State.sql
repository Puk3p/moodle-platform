-- Read state for chat messages, kept on the server so it survives sign-out, reconnects and a
-- change of device. It used to live in the browser's localStorage and was wiped at sign-out, so
-- every message came back as unread on the next login.
--
-- read_at is when the recipient opened the conversation; NULL means unread.
ALTER TABLE chat_messages ADD COLUMN read_at DATETIME NULL;

-- Messages sent before this change have no record of being read. Treat them as read, so nobody's
-- first login after the upgrade shows their whole history as new.
UPDATE chat_messages SET read_at = COALESCE(timestamp, CURRENT_TIMESTAMP) WHERE read_at IS NULL;

-- Serves "mark this conversation read" and the unread lookups.
CREATE INDEX idx_chat_messages_unread ON chat_messages (recipient, sender, read_at);
