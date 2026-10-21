#!/bin/bash
set -eu
# Run by the official MySQL entrypoint only when initializing an empty volume.
# MYSQL_USER already has privileges on MYSQL_DATABASE; create the migration user too.
: "${FLYWAY_USER:?Set FLYWAY_USER in .env}"
: "${FLYWAY_PASSWORD:?Set FLYWAY_PASSWORD in .env}"
sql_string() {
    local value=${1//\\/\\\\}
    value=${value//\'/\'\'}
    printf "'%s'" "$value"
}
flyway_user_sql=$(sql_string "$FLYWAY_USER")
flyway_password_sql=$(sql_string "$FLYWAY_PASSWORD")
schema_sql=${MYSQL_DATABASE//\`/\`\`}
MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=socket -uroot <<SQL
SET SESSION sql_mode = '';
CREATE USER IF NOT EXISTS $flyway_user_sql@'%' IDENTIFIED BY $flyway_password_sql;
GRANT ALL PRIVILEGES ON \`$schema_sql\`.* TO $flyway_user_sql@'%';
SQL
unset flyway_user_sql flyway_password_sql schema_sql
