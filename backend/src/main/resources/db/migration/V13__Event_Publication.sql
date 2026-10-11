-- Spring Modulith's event publication registry (the transactional outbox). An event that other
-- services must hear about is stored here in the same transaction as the change that caused it,
-- then sent to Kafka; rows without a completion date are retried. Schema as shipped by
-- spring-modulith-events-jdbc (schema-mysql.sql); the table name is upper case on purpose, the
-- library queries it that way and MySQL table names are case-sensitive on Linux.
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
