-- The monolith's sample grades and calendar entries (its V2), with the same ids. They belong to
-- the sample courses CS201 and CS350 (academic-service, ids 1 and 2) and the sample student Alex
-- (identity-service, id 1), which those services seed with the same ids. Dates are relative to
-- the day this runs, as they were.
--
-- Only on a new database: one that already holds grades or calendar entries (in use, or being
-- filled from the monolith by deploy/data-moves) is left as it is.

DELIMITER $$

CREATE PROCEDURE seed_samples()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM grades) AND NOT EXISTS (SELECT 1 FROM calendar_events) THEN

        INSERT INTO calendar_events (id, course_id, title, event_date, event_type)
        VALUES (1, 1, 'Lab 4 Submission', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 'lab'),
               (2, 1, 'Quiz 2', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 'quiz'),
               (3, 2, 'Project Proposal', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 'project');

        INSERT INTO grades (id, user_id, course_id, item_name, score_received, max_score,
                            weight_label, graded_at, type_icon)
        VALUES (1, 1, 1, 'Quiz 1', 18.5, 20.0, '10% of final', DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 'quiz'),
               (2, 1, 2, 'Lab 1', 10.0, 10.0, '5% of final', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'lab');
    END IF;
END $$

DELIMITER ;

CALL seed_samples();
DROP PROCEDURE seed_samples;
