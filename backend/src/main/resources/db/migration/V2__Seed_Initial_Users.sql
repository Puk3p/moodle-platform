INSERT INTO classes (name) VALUES ('1209A');
INSERT INTO classes (name) VALUES ('1210B');


-- STUDENT
INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('student@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Alex', 'Johnson', 1, 1);

-- PROFESOR
INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('teacher@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Eleanor', 'Vance', NULL, 1);

-- ADMIN
INSERT INTO users (email, password_hash, first_name, last_name, class_id, active)
VALUES ('admin@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Admin', 'User', NULL, 1);

--  Roluri
INSERT INTO user_roles (user_id, role) VALUES (1, 'STUDENT');
INSERT INTO user_roles (user_id, role) VALUES (2, 'TEACHER');
INSERT INTO user_roles (user_id, role) VALUES (3, 'ADMIN');

--  Cursuri
INSERT INTO courses (code, name, description, term, instructor_name, image_url) VALUES
                                                                                    ('CS201', 'Data Structures & Algorithms', 'Fundamental concepts of data structures.', 'Fall 2024', 'Prof. Eleanor Vance', 'https://img.freepik.com/free-vector/gradient-abstract-background_23-2149121815.jpg'),
                                                                                    ('CS350', 'Operating Systems', 'Concepts of OS, processes and threads.', 'Fall 2024', 'Dr. Ben Carter', 'https://img.freepik.com/free-vector/clean-gradient-background_23-2149132549.jpg');

-- Inscrieri
INSERT INTO enrollments (user_id, course_id, status) VALUES (1, 1, 'ACTIVE');
INSERT INTO enrollments (user_id, course_id, status) VALUES (1, 2, 'ACTIVE');

-- Module
INSERT INTO course_modules (course_id, title, description, sort_order) VALUES
                                                                           (1, 'Module 1 · Introduction', 'Basic array operations and complexity.', 1),
                                                                           (1, 'Module 2 · Linked Lists', 'Singly and doubly linked lists.', 2);

-- itemi pentru Module
INSERT INTO module_items (module_id, title, type, file_type, file_size, url, sort_order) VALUES
                                                                                             (1, 'Lecture 1 · Slides', 'lecture', 'pdf', '1.2 MB', '/files/cs201/lec1.pdf', 1),
                                                                                             (1, 'Lecture 1 · Recording', 'lecture', 'video', NULL, 'https://youtube.com/fake-link', 2),
                                                                                             (1, 'Lab 1 · Arrays', 'lab', 'zip', '5.8 MB', '/files/cs201/lab1.zip', 3);

-- Evenimente Calendar
INSERT INTO calendar_events (course_id, title, event_date, event_type) VALUES
                                                                           (1, 'Lab 4 Submission', DATE_ADD(CURRENT_DATE, INTERVAL 3 DAY), 'lab'),
                                                                           (1, 'Quiz 2', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 'quiz'),
                                                                           (2, 'Project Proposal', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), 'project');

-- Note
INSERT INTO grades (user_id, course_id, item_name, score_received, max_score, weight_label, graded_at, type_icon) VALUES
                                                                                                                      (1, 1, 'Quiz 1', 18.5, 20.0, '10% of final', DATE_SUB(CURRENT_DATE, INTERVAL 5 DAY), 'quiz'),
                                                                                                                      (1, 2, 'Lab 1', 10.0, 10.0, '5% of final', DATE_SUB(CURRENT_DATE, INTERVAL 10 DAY), 'lab');

-- Anunturi
INSERT INTO announcements (course_id, title, body, created_at) VALUES
    (1, 'Midterm Exam Info', 'Midterm will cover Modules 1-5.', NOW());