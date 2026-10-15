-- The same starter accounts and classes as core's V2, with the same ids: the seeded courses,
-- enrollments and grades in the other services refer to them. They share core's password hash.

INSERT INTO classes (id, name) VALUES (1, '1209A'), (2, '1210B');

INSERT INTO users (id, email, password_hash, first_name, last_name, class_id, active) VALUES
    (1, 'student@test.com',  '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Alex',    'Johnson', 1,    1),
    (2, 'teacher@test.com',  '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Eleanor', 'Vance',   NULL, 1),
    (3, 'admin@test.com',    '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Admin',   'User',    NULL, 1),
    (4, 'student4@test.com', '$2a$10$EuWPZHzz32dJN7jexM34MOeYirDdFAZm2mOWIVAWCRs/oTqF.b7C6', 'Student', 'Patru',   1,    1);

INSERT INTO user_roles (user_id, role) VALUES
    (1, 'STUDENT'),
    (2, 'TEACHER'),
    (3, 'ADMIN'),
    (4, 'STUDENT');
