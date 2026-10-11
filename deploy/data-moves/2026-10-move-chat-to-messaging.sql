-- One-time move of chat history from core's database (moodle) to messaging-service's (messaging),
-- for an installation that ran chat inside core. Run it as root on the MySQL server after
-- messaging-service has started once (Flyway creates messaging.chat_messages) and before users
-- are let back in:
--
--   docker compose -f deploy/compose.yml exec -T mysql \
--     sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < deploy/data-moves/2026-10-move-chat-to-messaging.sql
--
-- Safe to run twice (rows already copied are skipped). Ids are kept, so read markers and the
-- client's de-duplication still line up; MySQL continues AUTO_INCREMENT above the highest id.
-- core's own chat_messages table is left untouched; drop it once the move has been checked.
INSERT INTO messaging.chat_messages (id, sender, recipient, content, is_private, timestamp, read_at)
SELECT m.id, m.sender, m.recipient, m.content, m.is_private, m.timestamp, m.read_at
FROM moodle.chat_messages m
WHERE NOT EXISTS (SELECT 1 FROM messaging.chat_messages x WHERE x.id = m.id);

SELECT (SELECT COUNT(*) FROM moodle.chat_messages)    AS rows_in_core,
       (SELECT COUNT(*) FROM messaging.chat_messages) AS rows_in_messaging;
