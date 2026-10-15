#!/bin/bash
# One MySQL server, one database and one account per service. Each account can reach only its own
# database: services never read each other's tables, they ask each other (REST) or listen (Kafka).
#
# Idempotent. Runs on first start (docker-entrypoint-initdb.d) and on every start through the
# db-init service, so a newly extracted service gets its database on an existing installation.
set -euo pipefail
mysql ${MYSQL_HOST:+-h "$MYSQL_HOST"} -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS moodle;
CREATE DATABASE IF NOT EXISTS messaging;
CREATE DATABASE IF NOT EXISTS progress;
CREATE DATABASE IF NOT EXISTS assessment;
CREATE USER IF NOT EXISTS 'core'@'%' IDENTIFIED BY '${CORE_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'messaging'@'%' IDENTIFIED BY '${MESSAGING_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'progress'@'%' IDENTIFIED BY '${PROGRESS_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'assessment'@'%' IDENTIFIED BY '${ASSESSMENT_DB_PASSWORD}';
GRANT ALL PRIVILEGES ON moodle.* TO 'core'@'%';
GRANT ALL PRIVILEGES ON messaging.* TO 'messaging'@'%';
GRANT ALL PRIVILEGES ON progress.* TO 'progress'@'%';
GRANT ALL PRIVILEGES ON assessment.* TO 'assessment'@'%';
FLUSH PRIVILEGES;
SQL
echo "databases ready"
