-- ==========================================
-- A. CREARE TABELE (SCHEMA)
-- ==========================================

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
                         teacher_id      BIGINT,
                         status          VARCHAR(20) DEFAULT 'DRAFT',
                         image_url       VARCHAR(500),
                         created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL
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
                              is_visible    BOOLEAN NOT NULL DEFAULT TRUE,

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

-- ---------------------------------------------------------
-- C. SISTEM DE QUIZ (INSTANȚE CONCRETE ÎN CURS)
-- ---------------------------------------------------------

-- 13. QUIZZES
CREATE TABLE quizzes (
                         id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
                         course_id           BIGINT NOT NULL,
                         module_id           BIGINT NULL,
                         title               VARCHAR(255) NOT NULL,
                         description         TEXT,
                         status              VARCHAR(20) DEFAULT 'DRAFT',     -- 'PUBLISHED', 'DRAFT'
                         questions_count     INT DEFAULT 0,
                         duration_minutes    INT DEFAULT 30,
                         max_attempts        INT DEFAULT 1,                   -- 0 = Unlimited
                         passing_score       INT DEFAULT 50,

    -- COLONE NOI PENTRU CERINTELE AVANSATE:
                         shuffle_options     BOOLEAN DEFAULT FALSE,           -- 4) Shuffle optiuni
                         access_password     VARCHAR(50) DEFAULT NULL,        -- 7) Parola acces
                         available_from      TIMESTAMP NULL,                  -- 6) Pornire automata (start)
                         available_to        TIMESTAMP NULL,                  -- 6) Oprire automata (deadline)
                         generation_type     VARCHAR(20) DEFAULT 'MANUAL',    -- 'MANUAL' sau 'RANDOM' (2)

                         created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT fk_quizzes_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
                         CONSTRAINT fk_quizzes_module FOREIGN KEY (module_id) REFERENCES course_modules(id) ON DELETE SET NULL
);


-- 13.1 QUIZ ASSIGNED CLASSES (Pentru restrictionarea accesului la anumite clase)
CREATE TABLE quiz_assigned_classes (
                                       quiz_id  BIGINT NOT NULL,
                                       class_id BIGINT NOT NULL,
                                       PRIMARY KEY (quiz_id, class_id),
                                       CONSTRAINT fk_qac_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
                                       CONSTRAINT fk_qac_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE
);

-- 14. QUIZ QUESTIONS (Intrebarile specifice testului)
CREATE TABLE quiz_questions (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                quiz_id     BIGINT NOT NULL,
                                text        TEXT NOT NULL,
                                type        VARCHAR(50) NOT NULL,
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
                               status      VARCHAR(20) DEFAULT 'IN_PROGRESS',

                               CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
);

-- 17. QUIZ RESPONSES
CREATE TABLE quiz_responses (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                attempt_id  BIGINT NOT NULL,
                                question_id BIGINT NOT NULL,
                                selected_option_id BIGINT,
                                text_response      TEXT,

                                CONSTRAINT fk_response_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
                                CONSTRAINT fk_response_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE,
                                CONSTRAINT fk_response_option FOREIGN KEY (selected_option_id) REFERENCES quiz_options(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------
-- D. BANCA DE INTREBARI (QUESTION BANK - REUSABLE)
-- ---------------------------------------------------------

-- 18. CATEGORIES (Ierarhica)
CREATE TABLE categories (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            parent_id BIGINT,
                            sort_order INT DEFAULT 0,
                            CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 19. TAGS
CREATE TABLE tags (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(50) NOT NULL UNIQUE
);

-- 20. BANK QUESTIONS (Intrebarile generale din banca)
CREATE TABLE questions (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           category_id BIGINT,
                           text TEXT NOT NULL,
                           type VARCHAR(50) NOT NULL, -- 'CODE', 'MULTI_CHOICE', 'TRUE_FALSE', 'DRAG_DROP'
                           difficulty VARCHAR(20) NOT NULL, -- 'EASY', 'MEDIUM', 'HARD'
                           usage_count INT DEFAULT 0,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           image_url VARCHAR(500) DEFAULT NULL,

                           CONSTRAINT fk_bank_question_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- 21. QUESTION TAGS (Many-to-Many)
CREATE TABLE question_tags (
                               question_id BIGINT NOT NULL,
                               tag_id BIGINT NOT NULL,
                               PRIMARY KEY (question_id, tag_id),
                               CONSTRAINT fk_qt_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
                               CONSTRAINT fk_qt_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);

-- 22. BANK QUESTION OPTIONS
CREATE TABLE question_bank_options (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       question_id BIGINT NOT NULL,
                                       text VARCHAR(500) NOT NULL,
                                       is_correct BOOLEAN DEFAULT FALSE,
                                       sort_order INT DEFAULT 0,

                                       CONSTRAINT fk_bank_opt_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);