#!/usr/bin/env sh
set -eu
ENV_FILE="${ENV_FILE:-.env.prod}"
DUMP="${1:?Usage: database/restore.sh path/to/backup.sql.gz}"
[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE" >&2; exit 1; }
[ -s "$DUMP" ] || { echo "Backup file is missing or empty" >&2; exit 1; }
echo "WARNING: this imports SQL into the configured database and may overwrite existing data."
printf "Type RESTORE to continue: "; read answer
[ "$answer" = "RESTORE" ] || { echo "Cancelled"; exit 1; }
set -a; . "$ENV_FILE"; set +a
: "${MYSQL_DATABASE:?Missing MYSQL_DATABASE}"
: "${MYSQL_USER:?Missing MYSQL_USER}"
: "${MYSQL_PASSWORD:?Missing MYSQL_PASSWORD}"
gzip -dc "$DUMP" | docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T -e MYSQL_PWD="$MYSQL_PASSWORD" -e MYSQL_USER="$MYSQL_USER" mysql sh -c 'mysql -u"$MYSQL_USER" "$MYSQL_DATABASE"'
echo "Import completed. Verify application health and data consistency before reopening traffic."
