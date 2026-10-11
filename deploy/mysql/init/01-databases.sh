#!/bin/bash
# One MySQL server, one database and one account per service. Each account can reach only its own
# database: services never read each other's tables, they ask each other (REST) or listen (Kafka).
set -euo pipefail
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS moodle;
CREATE DATABASE IF NOT EXISTS messaging;
CREATE USER IF NOT EXISTS 'core'@'%' IDENTIFIED BY '${CORE_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'messaging'@'%' IDENTIFIED BY '${MESSAGING_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON moodle.* TO 'core'@'%';
GRANT ALL PRIVILEGES ON messaging.* TO 'messaging'@'%';
FLUSH PRIVILEGES;
SQL
