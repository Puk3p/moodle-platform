-- Courses, modules, resources and assignments, submissions, announcements and course membership,
-- moved out of the monolith's database. Same columns as its tables (its V1, V11, V13) so rows can
-- be copied over with their ids. Accounts and classes live in identity-service: teacher, student,
-- user and class ids are plain columns, and what their foreign keys did on deletion is done on
-- identity-service's identity.UserDeleted event (AccountHousekeeping).

CREATE TABLE courses
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    term        VARCHAR(50)  NOT NULL,
    teacher_id  BIGINT,
    status      VARCHAR(20) DEFAULT 'DRAFT',
    image_url   VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_courses_teacher ON courses (teacher_id);

-- Classes (identity-service) whose students all belong to the course.
CREATE TABLE course_classes
(
    course_id BIGINT NOT NULL,
    class_id  BIGINT NOT NULL,
    PRIMARY KEY (course_id, class_id),
    CONSTRAINT fk_cc_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);
CREATE INDEX idx_course_classes_class ON course_classes (class_id);

-- Students enrolled one by one, on top of those in assigned classes.
CREATE TABLE enrollments
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id   BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    status    VARCHAR(20) DEFAULT 'ACTIVE',
    CONSTRAINT uq_enrollment UNIQUE (user_id, course_id),
    CONSTRAINT fk_enroll_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE TABLE course_modules
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    start_date  DATE,
    end_date    DATE,
    CONSTRAINT fk_module_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

-- Resources, links and assignments (is_assignment) inside a module.
CREATE TABLE module_items
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    module_id       BIGINT       NOT NULL,
    title           VARCHAR(255) NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    file_type       VARCHAR(20),
    file_size       VARCHAR(20),
    url             VARCHAR(500),
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_assignment   BOOLEAN   DEFAULT FALSE,
    due_date        TIMESTAMP    NULL,
    is_visible      BOOLEAN      NOT NULL DEFAULT TRUE,
    max_grade       INT       DEFAULT 100,
    submission_type VARCHAR(20) DEFAULT 'Both',
    description     TEXT,
    CONSTRAINT fk_item_module FOREIGN KEY (module_id) REFERENCES course_modules (id) ON DELETE CASCADE
);

CREATE TABLE announcements
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id  BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_last    BOOLEAN   DEFAULT FALSE,
    CONSTRAINT fk_announcement_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

-- file_url holds the stored files' URLs joined by ';' (file_name likewise).
CREATE TABLE assignment_submissions
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_id    BIGINT NOT NULL,
    text_response TEXT,
    file_url      VARCHAR(500),
    file_name     VARCHAR(255),
    submitted_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    grade         INT,
    feedback      TEXT,
    CONSTRAINT fk_sub_assignment FOREIGN KEY (assignment_id) REFERENCES module_items (id) ON DELETE CASCADE
);
CREATE INDEX idx_submissions_student ON assignment_submissions (student_id);

-- Spring Modulith's event publication registry (the transactional outbox).
CREATE TABLE EVENT_PUBLICATION
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
