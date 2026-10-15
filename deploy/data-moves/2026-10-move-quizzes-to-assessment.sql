-- One-time move of quizzes, attempts, proctoring and the question bank from the monolith's
-- database (moodle) to assessment-service's (assessment), for an installation that ran them inside the monolith.
-- Run as root after assessment-service has started once (Flyway creates the tables):
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-quizzes-to-assessment.sql
--
-- Safe to run twice; ids are kept. Parents before children, so the foreign keys inside the
-- assessment database are satisfied. Question images: copy the files referenced by
-- questions.image_url from the core-uploads volume into assessment-uploads. The moodle tables are left untouched; drop
-- them once the move has been checked.

INSERT INTO assessment.quizzes (id, course_id, module_id, title, description, status, questions_count, duration_minutes, max_attempts, passing_score, shuffle_options, access_password, available_from, available_to, generation_type, block_messaging, created_at, updated_at)
SELECT s.id, s.course_id, s.module_id, s.title, s.description, s.status, s.questions_count, s.duration_minutes, s.max_attempts, s.passing_score, s.shuffle_options, s.access_password, s.available_from, s.available_to, s.generation_type, s.block_messaging, s.created_at, s.updated_at
FROM moodle.quizzes s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quizzes x WHERE x.id = s.id);

INSERT INTO assessment.quiz_assigned_classes (quiz_id, class_id)
SELECT s.quiz_id, s.class_id
FROM moodle.quiz_assigned_classes s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_assigned_classes x WHERE x.quiz_id = s.quiz_id AND x.class_id = s.class_id);

INSERT INTO assessment.quiz_questions (id, quiz_id, text, type, points, sort_order)
SELECT s.id, s.quiz_id, s.text, s.type, s.points, s.sort_order
FROM moodle.quiz_questions s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_questions x WHERE x.id = s.id);

INSERT INTO assessment.quiz_options (id, question_id, text, is_correct, sort_order)
SELECT s.id, s.question_id, s.text, s.is_correct, s.sort_order
FROM moodle.quiz_options s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_options x WHERE x.id = s.id);

INSERT INTO assessment.quiz_attempts (id, user_id, quiz_id, started_at, completed_at, score, status)
SELECT s.id, s.user_id, s.quiz_id, s.started_at, s.completed_at, s.score, s.status
FROM moodle.quiz_attempts s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_attempts x WHERE x.id = s.id);

INSERT INTO assessment.quiz_responses (id, attempt_id, question_id, selected_option_id, text_response, score)
SELECT s.id, s.attempt_id, s.question_id, s.selected_option_id, s.text_response, s.score
FROM moodle.quiz_responses s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_responses x WHERE x.id = s.id);

INSERT INTO assessment.quiz_extra_attempts (id, quiz_id, user_id, extra_attempts, granted_at)
SELECT s.id, s.quiz_id, s.user_id, s.extra_attempts, s.granted_at
FROM moodle.quiz_extra_attempts s
WHERE NOT EXISTS (SELECT 1 FROM assessment.quiz_extra_attempts x WHERE x.id = s.id);

INSERT INTO assessment.proctor_events (id, attempt_id, event_type, detail, client_ts, occurred_at)
SELECT s.id, s.attempt_id, s.event_type, s.detail, s.client_ts, s.occurred_at
FROM moodle.proctor_events s
WHERE NOT EXISTS (SELECT 1 FROM assessment.proctor_events x WHERE x.id = s.id);

INSERT INTO assessment.categories (id, name, parent_id, sort_order)
SELECT s.id, s.name, s.parent_id, s.sort_order
FROM moodle.categories s
WHERE NOT EXISTS (SELECT 1 FROM assessment.categories x WHERE x.id = s.id);

INSERT INTO assessment.tags (id, name)
SELECT s.id, s.name
FROM moodle.tags s
WHERE NOT EXISTS (SELECT 1 FROM assessment.tags x WHERE x.id = s.id);

INSERT INTO assessment.questions (id, category_id, text, type, difficulty, usage_count, created_at, image_url)
SELECT s.id, s.category_id, s.text, s.type, s.difficulty, s.usage_count, s.created_at, s.image_url
FROM moodle.questions s
WHERE NOT EXISTS (SELECT 1 FROM assessment.questions x WHERE x.id = s.id);

INSERT INTO assessment.question_tags (question_id, tag_id)
SELECT s.question_id, s.tag_id
FROM moodle.question_tags s
WHERE NOT EXISTS (SELECT 1 FROM assessment.question_tags x WHERE x.question_id = s.question_id AND x.tag_id = s.tag_id);

INSERT INTO assessment.question_bank_options (id, question_id, text, is_correct, sort_order)
SELECT s.id, s.question_id, s.text, s.is_correct, s.sort_order
FROM moodle.question_bank_options s
WHERE NOT EXISTS (SELECT 1 FROM assessment.question_bank_options x WHERE x.id = s.id);

SELECT (SELECT COUNT(*) FROM moodle.quizzes) AS quizzes_in_core,
       (SELECT COUNT(*) FROM assessment.quizzes) AS quizzes_in_assessment,
       (SELECT COUNT(*) FROM moodle.questions) AS questions_in_core,
       (SELECT COUNT(*) FROM assessment.questions) AS questions_in_assessment;
