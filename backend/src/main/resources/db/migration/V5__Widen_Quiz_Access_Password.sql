-- Widen access_password to hold BCrypt hashes (60 chars; 255 for future-proofing)
ALTER TABLE quizzes MODIFY access_password VARCHAR(255) DEFAULT NULL;

-- Rehash the seed plaintext password 'secret123' → BCrypt hash
-- Generated with BCryptPasswordEncoder(10): $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
UPDATE quizzes
SET access_password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE access_password = 'secret123';
