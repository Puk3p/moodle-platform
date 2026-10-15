-- The monolith's starter courses, with the same ids: identity-service seeds their teacher (2) and
-- students (1, 4), and the other services' seeds (grades, calendar, quizzes) refer to the courses
-- and modules.

INSERT INTO courses (id, code, name, description, term, teacher_id, image_url, status) VALUES
    (1, 'CS201', 'Data Structures & Algorithms', 'Fundamental concepts of data structures.', 'Fall 2026', 2, 'https://img.freepik.com/free-vector/gradient-abstract-background_23-2149121815.jpg', 'PUBLISHED'),
    (2, 'CS350', 'Operating Systems', 'Concepts of OS, processes and threads.', 'Fall 2026', 2, 'https://img.freepik.com/free-vector/clean-gradient-background_23-2149132549.jpg', 'DRAFT');

INSERT INTO enrollments (id, user_id, course_id, status) VALUES
    (1, 1, 1, 'ACTIVE'),
    (2, 1, 2, 'ACTIVE'),
    (3, 4, 1, 'ACTIVE');

INSERT INTO course_modules (id, course_id, title, description, sort_order, start_date, end_date) VALUES
    (1, 1, 'Module 1 · Introduction', 'Basic array operations and complexity.', 1, DATE_SUB(CURRENT_DATE, INTERVAL 2 WEEK), DATE_SUB(CURRENT_DATE, INTERVAL 1 WEEK)),
    (2, 1, 'Module 2 · Linked Lists', 'Singly and doubly linked lists.', 2, DATE_SUB(CURRENT_DATE, INTERVAL 1 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 6 DAY));

INSERT INTO module_items (id, module_id, title, type, file_type, file_size, url, sort_order, created_at, is_assignment, due_date, is_visible, max_grade, submission_type, description) VALUES
    (1, 1, 'Course_Syllabus_Fall2024.pdf', 'resource', 'pdf', '2.4 MB', '/files/syllabus.pdf', 1, '2026-10-24 10:00:00', FALSE, NULL, TRUE, 100, 'Both', NULL),
    (2, 1, 'Lecture 1 · Slides.pptx', 'resource', 'pptx', '12.8 MB', '/files/lec1.pptx', 2, '2026-10-25 14:30:00', FALSE, NULL, TRUE, 100, 'Both', NULL),
    (3, 1, 'Lecture 1 · Recording', 'lecture', 'video', NULL, 'https://youtube.com/fake-link', 3, NOW(), FALSE, NULL, TRUE, 100, 'Both', NULL),
    (4, 2, 'Data Structures VisuAlgo', 'resource', 'link', 'URL', 'https://visualgo.net', 1, '2026-10-26 09:15:00', FALSE, NULL, TRUE, NULL, NULL, NULL),
    (5, 2, 'Lab 1 · Arrays Implementation', 'assignment', 'zip', '5.8 MB', '/files/lab1.zip', 2, NOW(), TRUE, DATE_ADD(CURRENT_DATE, INTERVAL 2 DAY), TRUE, 100, 'File', 'Please upload your Java project archived as ZIP.'),
    (6, 2, 'Project_Starter_Code.zip', 'resource', 'zip', '14.2 MB', '/files/project.zip', 3, '2026-11-01 16:00:00', FALSE, NULL, TRUE, NULL, NULL, NULL),
    (7, 2, 'Hidden Instructor Notes.docx', 'resource', 'docx', '1.2 MB', '/files/notes.docx', 4, NOW(), FALSE, NULL, FALSE, NULL, NULL, NULL);

INSERT INTO announcements (id, course_id, title, body, created_at) VALUES
    (1, 1, 'Midterm Exam Info', 'Midterm will cover Modules 1-5.', NOW());
