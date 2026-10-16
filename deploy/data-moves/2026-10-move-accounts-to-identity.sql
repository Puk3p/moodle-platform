-- One-time move of accounts, classes, sessions and profile pictures from the monolith's database
-- (moodle) to identity-service's (identity), for an installation that ran them inside the monolith.
-- Run as root after identity-service has started once (Flyway creates the tables and the sample
-- accounts) and BEFORE the gateway sends it any traffic: an account registered in the meantime
-- could take an id or email still in use here.
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-accounts-to-identity.sql
--
-- Ids are kept, so the user and class ids in course data still match. The sample accounts and
-- classes identity-service seeds (its V2: users 1-4, classes 1-2) are replaced by this
-- installation's own rows with the same ids (changed passwords, 2FA, renames), and the ones it had
-- deleted are removed again: an admin@test.com removed for its publicly known password must not
-- come back. identity ends up holding exactly what moodle held, sessions included, so nobody is
-- signed out. Running it again before any traffic changes nothing.
--
-- Picture files: copy the old core-profile-pictures volume into identity-profile-pictures, e.g.
--
--   docker run --rm -v moodle_core-profile-pictures:/from:ro -v moodle_identity-profile-pictures:/to \
--     alpine sh -c 'cp -a /from/. /to/'
--
-- The moodle tables are left untouched; drop them once the move has been checked.

-- 1. Samples this installation no longer has. Their roles and sessions go with them; a sample
--    class's students keep their accounts, without a class. Also frees their email or class name
--    for an account or class the installation created later.
DELETE FROM identity.users WHERE id BETWEEN 1 AND 4 AND id NOT IN (SELECT id FROM moodle.users);
DELETE FROM identity.classes WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.classes);

-- 2. Everything moodle holds; where an id is already taken (a sample), moodle's row wins. In id
--    order, so a sample's row is updated before a later row could claim its email or name.
INSERT INTO identity.classes (id, name)
SELECT c.id, c.name
FROM moodle.classes c
ORDER BY c.id
ON DUPLICATE KEY UPDATE name = c.name;

INSERT INTO identity.users (id, email, password_hash, first_name, last_name, class_id, active, created_at, updated_at, two_fa_secret, two_fa_enabled)
SELECT u.id, u.email, u.password_hash, u.first_name, u.last_name, u.class_id, u.active, u.created_at, u.updated_at, u.two_fa_secret, COALESCE(u.two_fa_enabled, FALSE)
FROM moodle.users u
ORDER BY u.id
ON DUPLICATE KEY UPDATE email = u.email, password_hash = u.password_hash, first_name = u.first_name, last_name = u.last_name, class_id = u.class_id, active = u.active, created_at = u.created_at, updated_at = u.updated_at, two_fa_secret = u.two_fa_secret, two_fa_enabled = COALESCE(u.two_fa_enabled, FALSE);

-- Roles as this installation has them, replacing the seeded ones.
DELETE FROM identity.user_roles WHERE user_id IN (SELECT id FROM moodle.users);
INSERT INTO identity.user_roles (user_id, role)
SELECT r.user_id, r.role
FROM moodle.user_roles r;

INSERT INTO identity.user_sessions (id, user_id, device_name, ip_address, token_signature, last_active, user_agent_hash, created_at, expires_at)
SELECT s.id, s.user_id, s.device_name, s.ip_address, s.token_signature, s.last_active, s.user_agent_hash, s.created_at, s.expires_at
FROM moodle.user_sessions s
WHERE NOT EXISTS (SELECT 1 FROM identity.user_sessions x WHERE x.id = s.id);

INSERT INTO identity.password_reset_tokens (id, token, user_id, expiry_date)
SELECT t.id, t.token, t.user_id, t.expiry_date
FROM moodle.password_reset_tokens t
WHERE NOT EXISTS (SELECT 1 FROM identity.password_reset_tokens x WHERE x.id = t.id);

INSERT INTO identity.profile_pictures (id, user_id, storage_key, content_type, size_bytes, sha256, created_at, retired_at, purge_after)
SELECT p.id, p.user_id, p.storage_key, p.content_type, p.size_bytes, p.sha256, p.created_at, p.retired_at, p.purge_after
FROM moodle.profile_pictures p
WHERE NOT EXISTS (SELECT 1 FROM identity.profile_pictures x WHERE x.id = p.id);

-- 3. The two sides should now match.
SELECT (SELECT COUNT(*) FROM moodle.users)        AS users_in_moodle,
       (SELECT COUNT(*) FROM identity.users)      AS users_in_identity,
       (SELECT COUNT(*) FROM moodle.classes)      AS classes_in_moodle,
       (SELECT COUNT(*) FROM identity.classes)    AS classes_in_identity,
       (SELECT COUNT(*) FROM moodle.user_roles)   AS roles_in_moodle,
       (SELECT COUNT(*) FROM identity.user_roles) AS roles_in_identity;
