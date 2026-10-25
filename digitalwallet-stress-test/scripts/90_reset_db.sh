#!/usr/bin/env bash
# Reset DB. Yêu cầu app đã dừng (script tự kill theo $APP_PID_FILE).
set -euo pipefail
source "$(dirname "$0")/_common.sh"

# 1. Kill app nếu còn.
if [ -f "$APP_PID_FILE" ]; then
  PID=$(cat "$APP_PID_FILE")
  kill "$PID" 2>/dev/null || true
  for _ in $(seq 1 15); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
  kill -9 "$PID" 2>/dev/null || true
  rm -f "$APP_PID_FILE"
fi

# 2. Drop + tạo lại DB. DROP DATABASE tự xoá grant → phải re-grant.
mysql -u"$MYSQL_ROOT_USER" <<SQL
DROP DATABASE IF EXISTS ${DB_NAME};
CREATE DATABASE ${DB_NAME} CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
mysql -u"$MYSQL_ROOT_USER" -e "SHOW DATABASES;"