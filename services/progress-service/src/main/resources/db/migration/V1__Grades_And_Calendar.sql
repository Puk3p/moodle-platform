-- Grades and course calendars, moved out of core's database. Same shape as core's tables (V1) so
-- rows can be copied over; users and courses live in other services now, so their ids are plain
-- columns without foreign keys (deleted accounts are cleaned up from core's events).

CREATE TABLE IF NOT EXISTS calendar_events
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id   BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    event_date  DATE         NOT NULL,
    event_type  VARCHAR(50),
    description TEXT,
    -- What produced the entry (e.g. assignment 42), so a repeated event updates it instead of
    -- adding a duplicate. NULL for entries copied from core, which did not record it.
    source_type VARCHAR(30)  NULL,
    source_id   BIGINT       NULL,
    CONSTRAINT uq_calendar_source UNIQUE (source_type, source_id)
);
CREATE INDEX idx_calendar_course ON calendar_events (course_id, event_date);

CREATE TABLE IF NOT EXISTS grades
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    course_id      BIGINT       NOT NULL,
    item_name      VARCHAR(255) NOT NULL,
    score_received DECIMAL(5, 2),
    max_score      DECIMAL(5, 2),
    weight_label   VARCHAR(50),
    graded_at      DATE,
    type_icon      VARCHAR(20)
);
CREATE INDEX idx_grades_user ON grades (user_id, graded_at);
