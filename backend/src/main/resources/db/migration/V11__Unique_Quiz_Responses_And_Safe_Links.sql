-- 1. One graded response per question per attempt.
--
-- Grading used to score every answer a client sent, so an attempt could hold several responses for
-- the same question (one per option) and collect points for each. The engine now keeps only the
-- first answer per question; this removes the extra rows already stored, keeping the earliest
-- (lowest id) per (attempt_id, question_id), and lets the database refuse new duplicates.
--
-- The ids to keep are read through a derived table: MySQL rejects a subquery that reads the table
-- being deleted from directly (error 1093), and a derived table with GROUP BY is always
-- materialised first, never merged back into the DELETE. A self-join multi-table DELETE is not
-- used because MySQL 8.0 was seen to leave some duplicates behind with it.
DELETE FROM quiz_responses
WHERE id NOT IN (
    SELECT keep_id
    FROM (
        SELECT MIN(id) AS keep_id
        FROM quiz_responses
        GROUP BY attempt_id, question_id
    ) AS keepers
);

ALTER TABLE quiz_responses
    ADD CONSTRAINT uq_quiz_response_attempt_question UNIQUE (attempt_id, question_id);

-- 2. Link resources may only point to http(s) URLs.
--
-- Links are rendered as clickable anchors for every student in the course; a javascript: or data:
-- URL stored before validation existed would run in their session when clicked. Such links are
-- emptied and hidden so a teacher can see them in the course editor and re-enter a real address.
-- With the default case-insensitive collations HTTPS:// also counts as valid; under a
-- case-sensitive one such a link would merely be hidden, which errs on the safe side.
UPDATE module_items
SET url = '',
    is_visible = FALSE
WHERE file_type = 'link'
  AND (url IS NULL
       OR (url NOT LIKE 'http://%' AND url NOT LIKE 'https://%'));
