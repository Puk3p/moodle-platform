-- One-time move of courses, modules, resources, assignments, submissions, announcements and course
-- membership from the monolith's database (moodle) to academic-service's (academic). Run as root
-- after academic-service has started once (Flyway creates the tables and the sample courses) and
-- BEFORE the gateway sends it any traffic: a course created in the meantime could take an id still
-- in use here.
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-courses-to-academic.sql
--
-- Ids are kept, so the course and module ids other services hold still match. The samples
-- academic-service seeds (its V2: courses 1-2 with their modules, items, enrollments and
-- announcement) are replaced by this installation's own rows with the same ids, and the ones it
-- had deleted are removed again, so academic ends up holding exactly what moodle held. Running it
-- again before any traffic changes nothing. Parents before children, so the foreign keys inside
-- the academic database are satisfied.
--
-- Uploaded files (resources, submissions): copy the old core-uploads volume into academic-uploads,
-- e.g.
--
--   docker run --rm -v moodle_core-uploads:/from:ro -v moodle_academic-uploads:/to \
--     alpine sh -c 'cp -a /from/. /to/'
--
-- The moodle database is left untouched; drop it once every move has been checked.

-- 1. Samples this installation no longer has. Deleting a course or module takes its children; it
--    also frees a sample's course code for a course the installation created later.
DELETE FROM academic.courses WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.courses);
DELETE FROM academic.course_modules WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.course_modules);
DELETE FROM academic.module_items WHERE id BETWEEN 1 AND 7 AND id NOT IN (SELECT id FROM moodle.module_items);
DELETE FROM academic.enrollments WHERE id BETWEEN 1 AND 3 AND id NOT IN (SELECT id FROM moodle.enrollments);
DELETE FROM academic.announcements WHERE id = 1 AND id NOT IN (SELECT id FROM moodle.announcements);

-- 2. Everything moodle holds; where an id is already taken (a sample), moodle's row wins. Courses
--    in id order, so a sample's row is updated before a later course could claim its code.
INSERT INTO academic.courses (id, code, name, description, term, teacher_id, status, image_url, created_at)
SELECT c.id, c.code, c.name, c.description, c.term, c.teacher_id, c.status, c.image_url, c.created_at
FROM moodle.courses c
ORDER BY c.id
ON DUPLICATE KEY UPDATE code = c.code, name = c.name, description = c.description, term = c.term, teacher_id = c.teacher_id, status = c.status, image_url = c.image_url, created_at = c.created_at;

INSERT INTO academic.course_classes (course_id, class_id)
SELECT cc.course_id, cc.class_id
FROM moodle.course_classes cc
WHERE NOT EXISTS (SELECT 1 FROM academic.course_classes x WHERE x.course_id = cc.course_id AND x.class_id = cc.class_id);

INSERT INTO academic.enrollments (id, user_id, course_id, status)
SELECT e.id, e.user_id, e.course_id, e.status
FROM moodle.enrollments e
ORDER BY e.id
ON DUPLICATE KEY UPDATE user_id = e.user_id, course_id = e.course_id, status = e.status;

INSERT INTO academic.course_modules (id, course_id, title, description, sort_order, start_date, end_date)
SELECT m.id, m.course_id, m.title, m.description, m.sort_order, m.start_date, m.end_date
FROM moodle.course_modules m
ON DUPLICATE KEY UPDATE course_id = m.course_id, title = m.title, description = m.description, sort_order = m.sort_order, start_date = m.start_date, end_date = m.end_date;

INSERT INTO academic.module_items (id, module_id, title, type, file_type, file_size, url, sort_order, created_at, is_assignment, due_date, is_visible, max_grade, submission_type, description)
SELECT i.id, i.module_id, i.title, i.type, i.file_type, i.file_size, i.url, i.sort_order, i.created_at, i.is_assignment, i.due_date, i.is_visible, i.max_grade, i.submission_type, i.description
FROM moodle.module_items i
ON DUPLICATE KEY UPDATE module_id = i.module_id, title = i.title, type = i.type, file_type = i.file_type, file_size = i.file_size, url = i.url, sort_order = i.sort_order, created_at = i.created_at, is_assignment = i.is_assignment, due_date = i.due_date, is_visible = i.is_visible, max_grade = i.max_grade, submission_type = i.submission_type, description = i.description;

INSERT INTO academic.announcements (id, course_id, title, body, created_at, is_last)
SELECT a.id, a.course_id, a.title, a.body, a.created_at, a.is_last
FROM moodle.announcements a
ON DUPLICATE KEY UPDATE course_id = a.course_id, title = a.title, body = a.body, created_at = a.created_at, is_last = a.is_last;

INSERT INTO academic.assignment_submissions (id, assignment_id, student_id, text_response, file_url, file_name, submitted_at, grade, feedback)
SELECT s.id, s.assignment_id, s.student_id, s.text_response, s.file_url, s.file_name, s.submitted_at, s.grade, s.feedback
FROM moodle.assignment_submissions s
ON DUPLICATE KEY UPDATE assignment_id = s.assignment_id, student_id = s.student_id, text_response = s.text_response, file_url = s.file_url, file_name = s.file_name, submitted_at = s.submitted_at, grade = s.grade, feedback = s.feedback;

-- 3. The two sides should now match.
SELECT (SELECT COUNT(*) FROM moodle.courses)       AS courses_in_moodle,
       (SELECT COUNT(*) FROM academic.courses)     AS courses_in_academic,
       (SELECT COUNT(*) FROM moodle.module_items)  AS items_in_moodle,
       (SELECT COUNT(*) FROM academic.module_items) AS items_in_academic,
       (SELECT COUNT(*) FROM moodle.enrollments)   AS enrollments_in_moodle,
       (SELECT COUNT(*) FROM academic.enrollments) AS enrollments_in_academic;
