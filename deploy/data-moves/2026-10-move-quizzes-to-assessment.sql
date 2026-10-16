-- One-time move of quizzes, attempts, proctoring and the question bank from the monolith's
-- database (moodle) to assessment-service's (assessment), for an installation that ran them inside
-- the monolith. Run as root after assessment-service has started once (Flyway creates the tables
-- and the sample quizzes) and BEFORE the gateway sends it any traffic: a quiz created in the
-- meantime could take an id still in use here.
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-quizzes-to-assessment.sql
--
-- Ids are kept, so the quiz ids other services hold still match. The samples assessment-service
-- seeds (its V2) are replaced by this installation's own rows with the same ids, and the ones it
-- had deleted are removed again, so assessment ends up holding exactly what moodle held. Running it
-- again before any traffic changes nothing. Parents before children, so the foreign keys inside
-- the assessment database are satisfied.
--
-- Question images: copy the files referenced by questions.image_url from the core-uploads volume
-- into assessment-uploads. The moodle tables are left untouched; drop them once the move has been
-- checked.

-- 1. Samples this installation no longer has. Deleting a quiz or question takes its children.
DELETE FROM assessment.quizzes WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.quizzes);
DELETE FROM assessment.quiz_questions WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.quiz_questions);
DELETE FROM assessment.quiz_options WHERE id BETWEEN 1 AND 6 AND id NOT IN (SELECT id FROM moodle.quiz_options);
DELETE FROM assessment.quiz_attempts WHERE id = 1 AND id NOT IN (SELECT id FROM moodle.quiz_attempts);
DELETE FROM assessment.quiz_responses WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.quiz_responses);
DELETE FROM assessment.questions WHERE id BETWEEN 1 AND 6 AND id NOT IN (SELECT id FROM moodle.questions);
DELETE FROM assessment.question_bank_options WHERE id BETWEEN 1 AND 6 AND id NOT IN (SELECT id FROM moodle.question_bank_options);
DELETE FROM assessment.question_tags
WHERE (question_id, tag_id) IN ((1, 1), (1, 2), (2, 3), (3, 4), (3, 5), (4, 6), (4, 7), (5, 8), (6, 3))
  AND (question_id, tag_id) NOT IN (SELECT question_id, tag_id FROM moodle.question_tags);
DELETE FROM assessment.tags WHERE id BETWEEN 1 AND 8 AND id NOT IN (SELECT id FROM moodle.tags);
DELETE FROM assessment.categories WHERE id BETWEEN 1 AND 7 AND id NOT IN (SELECT id FROM moodle.categories);

-- 2. Everything moodle holds; where an id is already taken (a sample), moodle's row wins.
INSERT INTO assessment.quizzes (id, course_id, module_id, title, description, status, questions_count, duration_minutes, max_attempts, passing_score, shuffle_options, access_password, available_from, available_to, generation_type, block_messaging, created_at, updated_at)
SELECT s.id, s.course_id, s.module_id, s.title, s.description, s.status, s.questions_count, s.duration_minutes, s.max_attempts, s.passing_score, s.shuffle_options, s.access_password, s.available_from, s.available_to, s.generation_type, s.block_messaging, s.created_at, s.updated_at
FROM moodle.quizzes s
ON DUPLICATE KEY UPDATE course_id = s.course_id, module_id = s.module_id, title = s.title, description = s.description, status = s.status, questions_count = s.questions_count, duration_minutes = s.duration_minutes, max_attempts = s.max_attempts, passing_score = s.passing_score, shuffle_options = s.shuffle_options, access_password = s.access_password, available_from = s.available_from, available_to = s.available_to, generation_type = s.generation_type, block_messaging = s.block_messaging, created_at = s.created_at, updated_at = s.updated_at;

INSERT INTO assessment.quiz_assigned_classes (quiz_id, class_id)
SELECT s.quiz_id, s.class_id
FROM moodle.quiz_assigned_classes s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_assigned_classes x WHERE x.quiz_id = s.quiz_id AND x.class_id = s.class_id);

INSERT INTO assessment.quiz_questions (id, quiz_id, text, type, points, sort_order)
SELECT s.id, s.quiz_id, s.text, s.type, s.points, s.sort_order
FROM moodle.quiz_questions s
ON DUPLICATE KEY UPDATE quiz_id = s.quiz_id, text = s.text, type = s.type, points = s.points, sort_order = s.sort_order;

INSERT INTO assessment.quiz_options (id, question_id, text, is_correct, sort_order)
SELECT s.id, s.question_id, s.text, s.is_correct, s.sort_order
FROM moodle.quiz_options s
ON DUPLICATE KEY UPDATE question_id = s.question_id, text = s.text, is_correct = s.is_correct, sort_order = s.sort_order;

INSERT INTO assessment.quiz_attempts (id, user_id, quiz_id, started_at, completed_at, score, status)
SELECT s.id, s.user_id, s.quiz_id, s.started_at, s.completed_at, s.score, s.status
FROM moodle.quiz_attempts s
ON DUPLICATE KEY UPDATE user_id = s.user_id, quiz_id = s.quiz_id, started_at = s.started_at, completed_at = s.completed_at, score = s.score, status = s.status;

INSERT INTO assessment.quiz_responses (id, attempt_id, question_id, selected_option_id, text_response, score)
SELECT s.id, s.attempt_id, s.question_id, s.selected_option_id, s.text_response, s.score
FROM moodle.quiz_responses s
ON DUPLICATE KEY UPDATE attempt_id = s.attempt_id, question_id = s.question_id, selected_option_id = s.selected_option_id, text_response = s.text_response, score = s.score;

INSERT INTO assessment.quiz_extra_attempts (id, quiz_id, user_id, extra_attempts, granted_at)
SELECT s.id, s.quiz_id, s.user_id, s.extra_attempts, s.granted_at
FROM moodle.quiz_extra_attempts s
ON DUPLICATE KEY UPDATE quiz_id = s.quiz_id, user_id = s.user_id, extra_attempts = s.extra_attempts, granted_at = s.granted_at;

INSERT INTO assessment.proctor_events (id, attempt_id, event_type, detail, client_ts, occurred_at)
SELECT s.id, s.attempt_id, s.event_type, s.detail, s.client_ts, s.occurred_at
FROM moodle.proctor_events s
ON DUPLICATE KEY UPDATE attempt_id = s.attempt_id, event_type = s.event_type, detail = s.detail, client_ts = s.client_ts, occurred_at = s.occurred_at;

-- Categories nest: copy them without their parents first, so the order does not matter, then link.
INSERT INTO assessment.categories (id, name, parent_id, sort_order)
SELECT s.id, s.name, NULL, s.sort_order
FROM moodle.categories s
ON DUPLICATE KEY UPDATE name = s.name, parent_id = NULL, sort_order = s.sort_order;

UPDATE assessment.categories c
JOIN moodle.categories s ON s.id = c.id
SET c.parent_id = s.parent_id;

INSERT INTO assessment.tags (id, name)
SELECT s.id, s.name
FROM moodle.tags s
ON DUPLICATE KEY UPDATE name = s.name;

INSERT INTO assessment.questions (id, category_id, text, type, difficulty, usage_count, created_at, image_url)
SELECT s.id, s.category_id, s.text, s.type, s.difficulty, s.usage_count, s.created_at, s.image_url
FROM moodle.questions s
ON DUPLICATE KEY UPDATE category_id = s.category_id, text = s.text, type = s.type, difficulty = s.difficulty, usage_count = s.usage_count, created_at = s.created_at, image_url = s.image_url;

INSERT INTO assessment.question_tags (question_id, tag_id)
SELECT s.question_id, s.tag_id
FROM moodle.question_tags s
WHERE NOT EXISTS (SELECT 1 FROM assessment.question_tags x WHERE x.question_id = s.question_id AND x.tag_id = s.tag_id);

INSERT INTO assessment.question_bank_options (id, question_id, text, is_correct, sort_order)
SELECT s.id, s.question_id, s.text, s.is_correct, s.sort_order
FROM moodle.question_bank_options s
ON DUPLICATE KEY UPDATE question_id = s.question_id, text = s.text, is_correct = s.is_correct, sort_order = s.sort_order;

-- 3. The two sides should now match.
SELECT (SELECT COUNT(*) FROM moodle.quizzes)       AS quizzes_in_moodle,
       (SELECT COUNT(*) FROM assessment.quizzes)   AS quizzes_in_assessment,
       (SELECT COUNT(*) FROM moodle.questions)     AS questions_in_moodle,
       (SELECT COUNT(*) FROM assessment.questions) AS questions_in_assessment;
