-- One-time move of grades and course calendars from the monolith's database (moodle) to
-- progress-service's (progress), for an installation that ran them inside the monolith. Run as root
-- after progress-service has started once (Flyway creates the tables and the sample entries) and
-- BEFORE the gateway sends it any traffic: an entry created in the meantime could take an id still
-- in use here.
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-grades-and-calendar-to-progress.sql
--
-- Ids are kept. The samples progress-service seeds (its V2) are replaced by this installation's own
-- rows with the same ids, and the ones it had deleted are removed again, so progress ends up
-- holding exactly what moodle held. Running it again before any traffic changes nothing. Copied
-- calendar entries have no source, so an assignment published again later gets a fresh entry. The
-- moodle tables are left untouched; drop them once the move has been checked.

-- 1. Samples this installation no longer has.
DELETE FROM progress.calendar_events WHERE id BETWEEN 1 AND 3 AND id NOT IN (SELECT id FROM moodle.calendar_events);
DELETE FROM progress.grades WHERE id IN (1, 2) AND id NOT IN (SELECT id FROM moodle.grades);

-- 2. Everything moodle holds; where an id is already taken (a sample), moodle's row wins.
INSERT INTO progress.calendar_events (id, course_id, title, event_date, event_type, description)
SELECT c.id, c.course_id, c.title, c.event_date, c.event_type, c.description
FROM moodle.calendar_events c
ON DUPLICATE KEY UPDATE course_id = c.course_id, title = c.title, event_date = c.event_date, event_type = c.event_type, description = c.description, source_type = NULL, source_id = NULL;

INSERT INTO progress.grades (id, user_id, course_id, item_name, score_received, max_score,
                             weight_label, graded_at, type_icon)
SELECT g.id, g.user_id, g.course_id, g.item_name, g.score_received, g.max_score,
       g.weight_label, g.graded_at, g.type_icon
FROM moodle.grades g
ON DUPLICATE KEY UPDATE user_id = g.user_id, course_id = g.course_id, item_name = g.item_name, score_received = g.score_received, max_score = g.max_score, weight_label = g.weight_label, graded_at = g.graded_at, type_icon = g.type_icon;

-- 3. The two sides should now match.
SELECT (SELECT COUNT(*) FROM moodle.calendar_events)   AS calendar_in_moodle,
       (SELECT COUNT(*) FROM progress.calendar_events) AS calendar_in_progress,
       (SELECT COUNT(*) FROM moodle.grades)            AS grades_in_moodle,
       (SELECT COUNT(*) FROM progress.grades)          AS grades_in_progress;
