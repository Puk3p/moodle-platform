-- V5 set quizzes.access_password to a hard-coded BCrypt string that was copied from an example
-- rather than generated. Its comment claims the plaintext is 'secret123', but the hash verifies
-- against no known password (confirmed with a BCrypt comparison against 'secret123', 'password',
-- 'secret', 'quiz123', '123456'). Since quiz entry now uses passwordEncoder.matches(), any quiz
-- carrying that value can never be opened by anyone.
--
-- Clearing it removes the unusable gate and restores access. A teacher can set a real access
-- password from the UI afterwards, which is hashed correctly on write.
--
-- V5 is deliberately left untouched: applied migrations are immutable, so the correction lands
-- here and fixes existing and freshly-built databases alike.

UPDATE quizzes
SET access_password = NULL
WHERE access_password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
