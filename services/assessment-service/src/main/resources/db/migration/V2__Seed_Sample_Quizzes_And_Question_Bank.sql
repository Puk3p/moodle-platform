-- The monolith's sample quizzes, attempt and question bank (its V2, as its V5/V6 left them: the
-- "Random Weekly Quiz" has no access password), with the same ids. They belong to the sample
-- course CS201 and its first module (academic-service, ids 1) and the sample student Alex
-- (identity-service, id 1), which those services seed with the same ids.
--
-- Only on a new database: one that already holds quizzes, questions or categories (in use, or
-- being filled from the monolith by deploy/data-moves) is left as it is.

DELIMITER $$

CREATE PROCEDURE seed_samples()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM quizzes)
        AND NOT EXISTS (SELECT 1 FROM questions)
        AND NOT EXISTS (SELECT 1 FROM categories) THEN

        INSERT INTO quizzes (id, course_id, module_id, title, description, duration_minutes,
                             passing_score, status, questions_count, shuffle_options,
                             access_password, available_from, available_to, generation_type)
        VALUES (1, 1, 1, 'Quiz 1 · Complexity Basics', 'Test your knowledge on Big O notation.',
                15, 50, 'PUBLISHED', 2, FALSE, NULL, '2026-10-01 08:00:00', '2026-10-01 10:00:00',
                'MANUAL'),
               (2, 1, 1, 'Random Weekly Quiz', 'Generated from question bank.',
                10, 60, 'DRAFT', 0, TRUE, NULL, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 'RANDOM');

        INSERT INTO quiz_questions (id, quiz_id, text, type, points, sort_order)
        VALUES (1, 1, 'Ce complexitate are accesarea unui element in Array prin index?', 'SINGLE_CHOICE', 10, 1),
               (2, 1, 'Care structura functioneaza pe principiul LIFO?', 'SINGLE_CHOICE', 10, 2);

        INSERT INTO quiz_options (id, question_id, text, is_correct, sort_order)
        VALUES (1, 1, 'O(1)', TRUE, 1),
               (2, 1, 'O(n)', FALSE, 2),
               (3, 1, 'O(log n)', FALSE, 3),
               (4, 2, 'Queue', FALSE, 1),
               (5, 2, 'Stack', TRUE, 2),
               (6, 2, 'LinkedList', FALSE, 3);

        INSERT INTO quiz_attempts (id, user_id, quiz_id, started_at, completed_at, score, status)
        VALUES (1, 1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 20.00,
                'COMPLETED');

        INSERT INTO quiz_responses (id, attempt_id, question_id, selected_option_id)
        VALUES (1, 1, 1, 1),
               (2, 1, 2, 5);

        INSERT INTO categories (id, name, parent_id, sort_order)
        VALUES (1, 'All Questions', NULL, 0),
               (2, 'Week 1: Basics', 1, 1),
               (3, 'Data Structures', 1, 2),
               (4, 'Arrays & Lists', 3, 0),
               (5, 'Trees & Graphs', 3, 1),
               (6, 'Midterm Pool 2023', 1, 3),
               (7, 'Operating Systems', 1, 4);

        INSERT INTO tags (id, name)
        VALUES (1, 'javascript'), (2, 'web-dev'), (3, 'basics'), (4, 'css'), (5, 'frontend'),
               (6, 'advanced'), (7, 'algorithms'), (8, 'networking');

        INSERT INTO questions (id, text, type, difficulty, usage_count, category_id)
        VALUES (1, 'Explain the difference between ''=='' and ''==='' operators in JavaScript.', 'CODE', 'MEDIUM', 12, 2),
               (2, 'Which of the following is NOT a primitive data type?', 'MULTI_CHOICE', 'EASY', 45, 2),
               (3, 'CSS Grid is typically used for 1-dimensional layouts.', 'TRUE_FALSE', 'EASY', 5, 2),
               (4, 'Implement a Red-Black tree insertion algorithm.', 'CODE', 'HARD', 0, 5),
               (5, 'Match the network layers to their primary protocols (TCP/IP model).', 'DRAG_DROP', 'MEDIUM', 2, 7),
               (6, 'Describe the main principles of Object Oriented Programming in your own words.', 'FREE_TEXT', 'HARD', 0, 2);

        INSERT INTO question_tags (question_id, tag_id)
        VALUES (1, 1), (1, 2), (2, 3), (3, 4), (3, 5), (4, 6), (4, 7), (5, 8), (6, 3);

        INSERT INTO question_bank_options (id, question_id, text, is_correct, sort_order)
        VALUES (1, 2, 'String', TRUE, 1),
               (2, 2, 'boolean', FALSE, 2),
               (3, 2, 'int', FALSE, 3),
               (4, 2, 'char', FALSE, 4),
               (5, 3, 'True', FALSE, 1),
               (6, 3, 'False', TRUE, 2);
    END IF;
END $$

DELIMITER ;

CALL seed_samples();
DROP PROCEDURE seed_samples;
