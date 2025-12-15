

-- CLASE
CREATE TABLE classes (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         name VARCHAR(10) NOT NULL UNIQUE
);




-- UTILIZATORI
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

-- ROLURI
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role    VARCHAR(50) NOT NULL,
                            CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- CURSURI
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

-- INSCRIERI
CREATE TABLE enrollments (
                             id        BIGINT AUTO_INCREMENT PRIMARY KEY,
                             user_id   BIGINT NOT NULL,
                             course_id BIGINT NOT NULL,
                             status    VARCHAR(20) DEFAULT 'ACTIVE',

                             CONSTRAINT uq_enrollment UNIQUE (user_id, course_id),
                             CONSTRAINT fk_enroll_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- MODULE
CREATE TABLE course_modules (
                                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                course_id   BIGINT NOT NULL,
                                title       VARCHAR(255) NOT NULL,
                                description TEXT,
                                sort_order  INT NOT NULL DEFAULT 0,

                                CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- ITEMI MODULe
CREATE TABLE module_items (
                              id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                              module_id   BIGINT NOT NULL,
                              title       VARCHAR(255) NOT NULL,
                              type        VARCHAR(50) NOT NULL, -- lecture, lab, quiz, resource

                              -- detalii dspr resurselor
                              file_type   VARCHAR(20),  -- pdf, zip, link
                              file_size   VARCHAR(20),
                              url         VARCHAR(500),
                              sort_order  INT NOT NULL DEFAULT 0,

                              CONSTRAINT fk_item_module FOREIGN KEY (module_id) REFERENCES course_modules(id) ON DELETE CASCADE
);

-- CALENDAR EVENTS / DEADLINES
CREATE TABLE calendar_events (
                                 id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 course_id   BIGINT NOT NULL,
                                 title       VARCHAR(255) NOT NULL,
                                 event_date  DATE NOT NULL,
                                 event_type  VARCHAR(50), -- assignment, quiz, lab
                                 description TEXT,

                                 CONSTRAINT fk_calendar_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- NOTE
CREATE TABLE grades (
                        id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                        user_id        BIGINT NOT NULL,
                        course_id      BIGINT NOT NULL,

                        item_name      VARCHAR(255) NOT NULL, -- Quiz 1
                        score_received DECIMAL(5,2),
                        max_score      DECIMAL(5,2),
                        weight_label   VARCHAR(50), -- 15% of final
                        graded_at      DATE,
                        type_icon      VARCHAR(20), -- quiz, lab

                        CONSTRAINT fk_grade_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        CONSTRAINT fk_grade_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Announcements
CREATE TABLE announcements (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               course_id   BIGINT NOT NULL,
                               title       VARCHAR(255) NOT NULL,
                               body        TEXT NOT NULL,
                               created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               is_last     BOOLEAN DEFAULT FALSE, -- Pentru UI poate fi sters mai tz

                               CONSTRAINT fk_announcement_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- NOTIFICARI
CREATE TABLE notifications (
                               id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id     BIGINT NOT NULL,
                               title       VARCHAR(255) NOT NULL,
                               message     TEXT,
                               is_read     BOOLEAN DEFAULT FALSE,
                               created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);