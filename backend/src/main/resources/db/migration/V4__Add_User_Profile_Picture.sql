-- Profile picture URL for user accounts.
-- Reconstructed from the live schema on sentinel-vps (applied there 2026-03-28).

ALTER TABLE users
    ADD COLUMN profile_picture_url VARCHAR(512) NULL AFTER two_fa_enabled;
