-- 1. CLASE
CREATE TABLE classes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(10) NOT NULL UNIQUE
);

-- 2. UTILIZATORI
CREATE TABLE users (
                       id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name    VARCHAR(100) NOT NULL,
                       last_name     VARCHAR(100) NOT NULL,
                       class_id      BIGINT NULL,
                       active        BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       two_fa_secret VARCHAR(255) DEFAULT NULL,
                       two_fa_enabled BOOLEAN DEFAULT FALSE,

                       CONSTRAINT fk_users_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL
);

-- 3. ROLURI
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role    VARCHAR(50) NOT NULL,
                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. CURSURI
CREATE TABLE courses (
                         id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                         code            VARCHAR(20) NOT NULL UNIQUE,
                         name            VARCHAR(255) NOT NULL,
                         description     TEXT,
                         term            VARCHAR(50) NOT NULL,
                         instructor_name VARCHAR(100),
                         image_url       VARCHAR(500),
                         created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. INSCRIERI
CREATE TABLE enrollments (
                             id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id   BIGINT NOT NULL,
                             course_id BIGINT NOT NULL,
                             status    VARCHAR(20) DEFAULT 'ACTIVE',

                             CONSTRAINT uq_enrollment UNIQUE (user_id, course_id),
                             CONSTRAINT fk_enroll_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 6. MODULE
CREATE TABLE course_modules (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                course_id   BIGINT NOT NULL,
                                title       VARCHAR(255) NOT NULL,
                                description TEXT,
                                sort_order  INT NOT NULL DEFAULT 0,
                                start_date  DATE,
                                end_date    DATE,

                                CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 7. ITEMI MODULE
CREATE TABLE module_items (
                              id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                              module_id     BIGINT NOT NULL,
                              title         VARCHAR(255) NOT NULL,
                              type          VARCHAR(50) NOT NULL,
                              file_type     VARCHAR(20),
                              file_size     VARCHAR(20),
                              url           VARCHAR(500),
                              sort_order    INT NOT NULL DEFAULT 0,
                              created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              is_assignment BOOLEAN DEFAULT FALSE,
                              due_date      TIMESTAMP NULL,

                              CONSTRAINT fk_item_module FOREIGN KEY (module_id) REFERENCES course_modules(id) ON DELETE CASCADE
);

-- 8. CALENDAR EVENTS
CREATE TABLE calendar_events (
                                 id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 course_id   BIGINT NOT NULL,
                                 title       VARCHAR(255) NOT NULL,
                                 event_date  DATE NOT NULL,
                                 event_type  VARCHAR(50),
                                 description TEXT,

                                 CONSTRAINT fk_calendar_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 9. NOTE (GRADES)
CREATE TABLE grades (
                        id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id        BIGINT NOT NULL,
                        course_id      BIGINT NOT NULL,
                        item_name      VARCHAR(255) NOT NULL,
                        score_received DECIMAL(5,2),
                        max_score      DECIMAL(5,2),
                        weight_label   VARCHAR(50),
                        graded_at      DATE,
                        type_icon      VARCHAR(20),

                        CONSTRAINT fk_grade_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_grade_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 10. ANNOUNCEMENTS
CREATE TABLE announcements (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               course_id   BIGINT NOT NULL,
                               title       VARCHAR(255) NOT NULL,
                               body        TEXT NOT NULL,
                               created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               is_last     BOOLEAN DEFAULT FALSE,

                               CONSTRAINT fk_announcement_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 11. NOTIFICARI
CREATE TABLE notifications (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id     BIGINT NOT NULL,
                               title       VARCHAR(255) NOT NULL,
                               message     TEXT,
                               is_read     BOOLEAN DEFAULT FALSE,
                               created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 12. USER SESSIONS
CREATE TABLE user_sessions (
                               id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id         BIGINT NOT NULL,
                               device_name     VARCHAR(255),
                               ip_address      VARCHAR(50),
                               token_signature VARCHAR(500) NOT NULL,
                               last_active     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 13. QUIZZES (Testele)
CREATE TABLE quizzes (
                         id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                         course_id           BIGINT NOT NULL,
                         module_id           BIGINT NULL, -- Optional, legat de un modul
                         title               VARCHAR(255) NOT NULL,
                         description         TEXT,
                         status              VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, PUBLISHED
                         questions_count     INT DEFAULT 0,
                         duration_minutes    INT DEFAULT 30,
                         max_attempts        INT DEFAULT 1,
                         passing_score       INT DEFAULT 50, -- Punctaj minim
                         created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_quizzes_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
                         CONSTRAINT fk_quizzes_module FOREIGN KEY (module_id) REFERENCES course_modules(id) ON DELETE SET NULL
);

-- 14. QUIZ QUESTIONS
CREATE TABLE quiz_questions (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                quiz_id     BIGINT NOT NULL,
                                text        TEXT NOT NULL,
                                type        VARCHAR(50) NOT NULL, -- SINGLE_CHOICE, MULTIPLE_CHOICE
                                points      INT DEFAULT 1,
                                sort_order  INT DEFAULT 0,

                                CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- 15. QUIZ OPTIONS
CREATE TABLE quiz_options (
                              id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                              question_id BIGINT NOT NULL,
                              text        VARCHAR(500) NOT NULL,
                              is_correct  BOOLEAN DEFAULT FALSE,
                              sort_order  INT DEFAULT 0,

                              CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE
);

-- 16. QUIZ ATTEMPTS
CREATE TABLE quiz_attempts (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id     BIGINT NOT NULL,
                               quiz_id     BIGINT NOT NULL,
                               started_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               completed_at TIMESTAMP NULL,
                               score       DECIMAL(5,2),
                               status      VARCHAR(20) DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED

                               CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- 17. QUIZ RESPONSES
CREATE TABLE quiz_responses (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                attempt_id  BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                selected_option_id BIGINT,

                                CONSTRAINT fk_response_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
                                CONSTRAINT fk_response_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
                                CONSTRAINT fk_response_option FOREIGN KEY (selected_option_id) REFERENCES quiz_options(id) ON DELETE CASCADE
);