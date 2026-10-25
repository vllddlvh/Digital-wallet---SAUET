#!/usr/bin/env bash
# Reset DB + kill SUT (được phép — đây là 1 trong 3 script kill).
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

if [ -f "$APP_PID_FILE" ]; then
  PID=$(cat "$APP_PID_FILE")
  if kill -0 "$PID" 2>/dev/null; then
    log "Kill SUT pid=$PID"
    kill "$PID" 2>/dev/null || true
    for _ in $(seq 1 15); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
  rm -f "$APP_PID_FILE"
fi

mysql -u"$MYSQL_ROOT_USER" <<SQL
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

log "DB reset OK"
mysql -u"$MYSQL_ROOT_USER" -e "SHOW DATABASES;"