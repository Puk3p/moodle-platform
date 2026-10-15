-- One-time move of grades and course calendars from core's database (moodle) to progress-service's
-- (progress), for an installation that ran them inside core. Run as root after progress-service has
-- started once (Flyway creates the tables):
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-grades-and-calendar-to-progress.sql
--
-- Safe to run twice; ids are kept. Copied calendar entries have no source, so an assignment
-- published again later gets a fresh entry. core's tables are left untouched; drop them once the
-- move has been checked.
INSERT INTO progress.calendar_events (id, course_id, title, event_date, event_type, description)
SELECT c.id, c.course_id, c.title, c.event_date, c.event_type, c.description
FROM moodle.calendar_events c
WHERE NOT EXISTS (SELECT 1 FROM progress.calendar_events x WHERE x.id = c.id);

INSERT INTO progress.grades (id, user_id, course_id, item_name, score_received, max_score,
                             weight_label, graded_at, type_icon)
SELECT g.id, g.user_id, g.course_id, g.item_name, g.score_received, g.max_score,
       g.weight_label, g.graded_at, g.type_icon
FROM moodle.grades g
WHERE NOT EXISTS (SELECT 1 FROM progress.grades x WHERE x.id = g.id);

SELECT (SELECT COUNT(*) FROM moodle.calendar_events)   AS calendar_in_core,
       (SELECT COUNT(*) FROM progress.calendar_events) AS calendar_in_progress,
       (SELECT COUNT(*) FROM moodle.grades)            AS grades_in_core,
       (SELECT COUNT(*) FROM progress.grades)          AS grades_in_progress;
