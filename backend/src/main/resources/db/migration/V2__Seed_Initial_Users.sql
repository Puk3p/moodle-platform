-- 1. Clase
INSERT INTO classes (name) VALUES ('1209A');
INSERT INTO classes (name) VALUES ('1210B');

-- 2. Utilizatori
-- ATENTIE: Parola hash-uita este pentru 'password'
INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('student@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Alex', 'Johnson', 1, 1);

INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('teacher@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Eleanor', 'Vance', NULL, 1);

INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('admin@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Admin', 'User', NULL, 1);

INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('student4@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Student', 'Patru', 1, 1);

-- 3. Roluri
INSERT INTO user_roles (user_id, role) VALUES (1, 'STUDENT');
INSERT INTO user_roles (user_id, role) VALUES (2, 'TEACHER');
INSERT INTO user_roles (user_id, role) VALUES (3, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (4, 'STUDENT');

-- 4. Cursuri
INSERT INTO courses (code, name, description, term, instructor_name, image_url) VALUES
                                                                                    ('CS201', 'Data Structures & Algorithms', 'Fundamental concepts of data structures.', 'Fall 2024', 'Prof. Eleanor Vance', 'https://img.freepik.com/free-vector/gradient-abstract-background_23-2149121815.jpg'),
                                                                                    ('CS350', 'Operating Systems', 'Concepts of OS, processes and threads.', 'Fall 2024', 'Dr. Ben Carter', 'https://img.freepik.com/free-vector/clean-gradient-background_23-2149132549.jpg');

-- 5. Inscrieri
INSERT INTO enrollments (user_id, course_id, status) VALUES (1, 1, 'ACTIVE');
INSERT INTO enrollments (user_id, course_id, status) VALUES (1, 2, 'ACTIVE');
INSERT INTO enrollments (user_id, course_id, status) VALUES (4, 1, 'ACTIVE');

-- 6. Module
INSERT INTO course_modules (course_id, title, description, sort_order, start_date, end_date)
VALUES (1, 'Module 1 · Introduction', 'Basic array operations and complexity.', 1, DATE_SUB(CURRENT_DATE, INTERVAL 2 WEEK), DATE_SUB(CURRENT_DATE, INTERVAL 1 WEEK));

INSERT INTO course_modules (course_id, title, description, sort_order, start_date, end_date)
VALUES (1, 'Module 2 · Linked Lists', 'Singly and doubly linked lists.', 2, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY));

-- 7. Itemi Module
INSERT INTO module_items (module_id, title, type, file_type, file_size, url, sort_order, created_at)
VALUES
    (1, 'Course_Syllabus_Fall2024.pdf', 'resource', 'pdf', '2.4 MB', '/files/syllabus.pdf', 1, '2024-10-24 10:00:00'),
    (1, 'Lecture 1 · Slides.pptx', 'resource', 'pptx', '12.8 MB', '/files/lec1.pptx', 2, '2024-10-25 14:30:00'),
    (1, 'Lecture 1 · Recording', 'lecture', 'video', NULL, 'https://youtube.com/fake-link', 3, NOW());

INSERT INTO module_items (module_id, title, type, file_type, file_size, url, sort_order, created_at, is_assignment, due_date)
VALUES
    (2, 'Data Structures VisuAlgo', 'resource', 'link', 'URL', 'https://visualgo.net', 1, '2024-10-26 09:15:00', FALSE, NULL),
    (2, 'Lab 1 · Arrays Implementation', 'lab', 'zip', '5.8 MB', '/files/lab1.zip', 2, NOW(), TRUE, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY)),
    (2, 'Project_Starter_Code.zip', 'resource', 'zip', '14.2 MB', '/files/project.zip', 3, '2024-11-01 16:00:00', FALSE, NULL);

-- 8. Calendar
INSERT INTO calendar_events (course_id, title, event_date, event_type) VALUES
                                                                           (1, 'Lab 4 Submission', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 'lab'),
                                                                           (1, 'Quiz 2', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 'quiz'),
                                                                           (2, 'Project Proposal', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 'project');

-- 9. Note (Generic Grades)
INSERT INTO grades (user_id, course_id, item_name, score_received, max_score, weight_label, graded_at, type_icon) VALUES
                                                                                                                      (1, 1, 'Quiz 1', 18.5, 20.0, '10% of final', DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 'quiz'),
                                                                                                                      (1, 2, 'Lab 1', 10.0, 10.0, '5% of final', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'lab');

-- 10. Anunturi
INSERT INTO announcements (course_id, title, body, created_at) VALUES
    (1, 'Midterm Exam Info', 'Midterm will cover Modules 1-5.', NOW());


-- 13. Seed Quizzes (Pentru CS201)
INSERT INTO quizzes (course_id, module_id, title, description, duration_minutes, passing_score, status, questions_count)
VALUES (1, 1, 'Quiz 1 · Complexity Basics', 'Test your knowledge on Big O notation.', 15, 50, 'PUBLISHED', 2);

-- 14. Seed Questions
-- Intrebarea 1
INSERT INTO quiz_questions (quiz_id, text, type, points, sort_order)
VALUES (1, 'Ce complexitate are accesarea unui element in Array prin index?', 'SINGLE_CHOICE', 10, 1);

-- Intrebarea 2
INSERT INTO quiz_questions (quiz_id, text, type, points, sort_order)
VALUES (1, 'Care structura functioneaza pe principiul LIFO?', 'SINGLE_CHOICE', 10, 2);

-- 15. Seed Options
-- Pentru Intrebarea 1 (ID 1)
INSERT INTO quiz_options (question_id, text, is_correct, sort_order) VALUES
                                                                         (1, 'O(1)', TRUE, 1),
                                                                         (1, 'O(n)', FALSE, 2),
                                                                         (1, 'O(log n)', FALSE, 3);

-- Pentru Intrebarea 2 (ID 2)
INSERT INTO quiz_options (question_id, text, is_correct, sort_order) VALUES
                                                                         (2, 'Queue', FALSE, 1),
                                                                         (2, 'Stack', TRUE, 2),
                                                                         (2, 'LinkedList', FALSE, 3);

-- 16. Seed Attempts (Studentul a dat deja testul)
INSERT INTO quiz_attempts (user_id, quiz_id, started_at, completed_at, score, status)
VALUES (1, 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 45 MINUTE), 20.00, 'COMPLETED');

-- 17. Seed Responses (A raspuns corect la ambele)
INSERT INTO quiz_responses (attempt_id, question_id, selected_option_id) VALUES
                                                                             (1, 1, 1), -- O(1)
                                                                             (1, 2, 5); -- Stack (ID 5 este al doilea insert la a doua intrebare)