-- The RS256 key pair identity-service signs internal tokens with, unless one is configured
-- (security.internal-jwt.*). Generated on the first start and kept here, so a restart or a second
-- instance signs with the same key and no token in flight stops being valid. A single row.
--
-- Like the rest of this database (password hashes, 2FA secrets), it must stay private: only this
-- service's account can read it. Backups of it are as sensitive as the key itself.
CREATE TABLE signing_keys
(
    id          VARCHAR(20) PRIMARY KEY,
    private_key TEXT        NOT NULL,
    public_key  TEXT        NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
