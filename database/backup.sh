#!/usr/bin/env sh
set -eu
# Run from repository root with .env.prod present; output directory should be access-controlled.
ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "Missing $ENV_FILE; create it from docs/DEPLOYMENT.md" >&2; exit 1; }
set -a
. "$ENV_FILE"
set +a
: "${MYSQL_DATABASE:?Missing MYSQL_DATABASE}"
: "${MYSQL_USER:?Missing MYSQL_USER}"
: "${MYSQL_PASSWORD:?Missing MYSQL_PASSWORD}"
OUT_DIR="${BACKUP_DIR:-./backups}"
umask 077
mkdir -p "$OUT_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT="$OUT_DIR/${MYSQL_DATABASE}_$STAMP.sql.gz"
MYSQL_PWD="$MYSQL_PASSWORD" docker compose --env-file "$ENV_FILE" -f docker-compose.prod.yml exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PWD" mysqldump --single-transaction --routines --triggers --hex-blob -u"$MYSQL_USER" "$MYSQL_DATABASE"' | gzip -9 > "$OUT"
test -s "$OUT" || { rm -f "$OUT"; echo "Backup output is empty" >&2; exit 1; }
echo "Created $OUT"
