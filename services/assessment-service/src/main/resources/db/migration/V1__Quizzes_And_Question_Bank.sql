-- Quizzes, attempts, proctoring and the question bank, moved out of core's database. Same columns as
-- core's tables (its V1, V3, V5, V7, V8, V11) so rows can be copied over. Users, courses, modules and
-- classes live in other services now: their ids are plain columns, and what their foreign keys did
-- (cascade on user deletion, unfile on module removal) is done from their events.

CREATE TABLE quizzes
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id        BIGINT       NOT NULL,
    module_id        BIGINT       NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    status           VARCHAR(20)  DEFAULT 'DRAFT',
    questions_count  INT          DEFAULT 0,
    duration_minutes INT          DEFAULT 30,
    max_attempts     INT          DEFAULT 1,
    passing_score    INT          DEFAULT 50,
    shuffle_options  BOOLEAN      DEFAULT FALSE,
    access_password  VARCHAR(255) DEFAULT NULL,
    available_from   TIMESTAMP    NULL,
    available_to     TIMESTAMP    NULL,
    generation_type  VARCHAR(20)  DEFAULT 'MANUAL',
    block_messaging  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_quizzes_course ON quizzes (course_id);
CREATE INDEX idx_quizzes_module ON quizzes (module_id);

CREATE TABLE quiz_assigned_classes
(
    quiz_id  BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (quiz_id, class_id),
    CONSTRAINT fk_qac_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE quiz_questions
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id    BIGINT      NOT NULL,
    text       TEXT        NOT NULL,
    type       VARCHAR(50) NOT NULL,
    points     INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE quiz_options
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT       NOT NULL,
    text        VARCHAR(500) NOT NULL,
    is_correct  BOOLEAN DEFAULT FALSE,
    sort_order  INT     DEFAULT 0,
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE
);

CREATE TABLE quiz_attempts
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    quiz_id      BIGINT NOT NULL,
    started_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    score        DECIMAL(5, 2),
    status       VARCHAR(20) DEFAULT 'IN_PROGRESS',
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);
CREATE INDEX idx_attempts_user ON quiz_attempts (user_id, status);

CREATE TABLE quiz_responses
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id         BIGINT NOT NULL,
    question_id        BIGINT NOT NULL,
    selected_option_id BIGINT,
    text_response      TEXT,
    score              DECIMAL(5, 2) DEFAULT 0.00,
    CONSTRAINT uq_quiz_response_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_response_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE,
    CONSTRAINT fk_response_question FOREIGN KEY (question_id) REFERENCES quiz_questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_response_option FOREIGN KEY (selected_option_id) REFERENCES quiz_options (id) ON DELETE CASCADE
);

CREATE TABLE quiz_extra_attempts
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id        BIGINT    NOT NULL,
    user_id        BIGINT    NOT NULL,
    extra_attempts INT       NOT NULL DEFAULT 0,
    granted_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quiz_user UNIQUE (quiz_id, user_id),
    CONSTRAINT fk_qea_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes (id) ON DELETE CASCADE
);

CREATE TABLE proctor_events
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id  BIGINT      NOT NULL,
    event_type  VARCHAR(40) NOT NULL,
    detail      VARCHAR(255) NULL,
    client_ts   BIGINT       NULL,
    occurred_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proctor_attempt FOREIGN KEY (attempt_id) REFERENCES quiz_attempts (id) ON DELETE CASCADE
);
CREATE INDEX idx_proctor_attempt ON proctor_events (attempt_id, occurred_at);

-- Question bank
CREATE TABLE categories
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    parent_id  BIGINT,
    sort_order INT DEFAULT 0,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL
);

CREATE TABLE tags
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE questions
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT,
    text        TEXT        NOT NULL,
    type        VARCHAR(50) NOT NULL,
    difficulty  VARCHAR(20) NOT NULL,
    usage_count INT          DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    image_url   VARCHAR(500) DEFAULT NULL,
    CONSTRAINT fk_bank_question_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

CREATE TABLE question_tags
(
    question_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    PRIMARY KEY (question_id, tag_id),
    CONSTRAINT fk_qt_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_qt_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE question_bank_options
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT       NOT NULL,
    text        VARCHAR(500) NOT NULL,
    is_correct  BOOLEAN DEFAULT FALSE,
    sort_order  INT     DEFAULT 0,
    CONSTRAINT fk_bank_opt_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);

-- Spring Modulith's event publication registry (outbox), as in core.
CREATE TABLE IF NOT EXISTS EVENT_PUBLICATION
(
    ID               VARCHAR(36)   NOT NULL,
    LISTENER_ID      VARCHAR(512)  NOT NULL,
    EVENT_TYPE       VARCHAR(512)  NOT NULL,
    SERIALIZED_EVENT VARCHAR(4000) NOT NULL,
    PUBLICATION_DATE TIMESTAMP(6)  NOT NULL,
    COMPLETION_DATE  TIMESTAMP(6)  DEFAULT NULL NULL,
    PRIMARY KEY (ID),
    INDEX EVENT_PUBLICATION_BY_COMPLETION_DATE_IDX (COMPLETION_DATE)
);
