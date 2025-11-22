INSERT INTO users (email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES
    ('admin@moodlev2.com',
     '$2a$10$7QWPIx9nqzVxO3J4kNKO.uTDQf3dV/7VREu6P6QdxVX/CwEub2B8.', -- parola: admin123
     'System', 'Admin', TRUE, NOW(), NOW()
    ),
    ('teacher@moodlev2.com',
     '$2a$10$7QWPIx9nqzVxO3J4kNKO.uTDQf3dV/7VREu6P6QdxVX/CwEub2B8.', -- parola: admin123
     'John', 'Teacher', TRUE, NOW(), NOW()
    ),
    ('student@moodlev2.com',
     '$2a$10$7QWPIx9nqzVxO3J4kNKO.uTDQf3dV/7VREu6P6QdxVX/CwEub2B8.', -- parola: admin123
     'Alice', 'Student', TRUE, NOW(), NOW()
    );

INSERT INTO user_roles (user_id, role)
VALUES
    (1, 'ADMIN'),
    (2, 'TEACHER'),
    (3, 'STUDENT');