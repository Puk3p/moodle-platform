-- Sessions move from "hash of a JWT the browser keeps in JavaScript and sends as a Bearer header"
-- to "hash of an opaque random token in an HttpOnly, Secure, SameSite=Strict cookie", bound to the
-- browser that created it, with server-side idle and absolute expiry.
--
-- Existing rows hold hashes of the old bearer tokens and can never match again, so they are
-- removed: everyone signs in once more after this release.
DELETE FROM user_sessions;

-- NULL DEFAULT NULL on purpose: on MariaDB/older MySQL a bare "TIMESTAMP NOT NULL" column can be
-- given an implicit ON UPDATE CURRENT_TIMESTAMP, which would silently move created_at/expires_at
-- every time last_active is touched. The application always sets these values.
ALTER TABLE user_sessions
    ADD COLUMN user_agent_hash VARCHAR(64) NULL DEFAULT NULL,
    ADD COLUMN created_at      TIMESTAMP   NULL DEFAULT NULL,
    ADD COLUMN expires_at      TIMESTAMP   NULL DEFAULT NULL;

-- token_signature is now always a SHA-256 hex digest, looked up on every request.
ALTER TABLE user_sessions MODIFY token_signature VARCHAR(64) NOT NULL;
CREATE UNIQUE INDEX uq_user_session_token ON user_sessions (token_signature);

-- Password reset tokens are now stored as SHA-256 hashes rather than in plain text. Outstanding
-- plain-text tokens (valid for an hour at most) are dropped; users can request a new link.
DELETE FROM password_reset_tokens;
