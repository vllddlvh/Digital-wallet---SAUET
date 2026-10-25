#!/usr/bin/env bash
# Khởi động MySQL và tạo database + user. Idempotent.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

: "${MYSQL_DATA:=/var/lib/mysql}"
: "${MYSQL_RUN:=/var/run/mysqld}"
: "${MYSQL_LOG:=/tmp/mysql.log}"

mkdir -p "$MYSQL_RUN"
chown -R mysql:mysql "$MYSQL_RUN" 2>/dev/null || true

# Initialize datadir nếu cần.
if [ ! -d "$MYSQL_DATA/mysql" ]; then
  rm -rf "${MYSQL_DATA:?}"/*
  mysqld --initialize-insecure --user=mysql
fi

# Start background nếu chưa chạy.
if ! db_alive; then
  setsid nohup mysqld --user=root \
    --bind-address=127.0.0.1 --mysqlx=OFF \
    --max-connections=500 --innodb-buffer-pool-size=2G \
    < /dev/null > "$MYSQL_LOG" 2>&1 &
  for _ in $(seq 1 60); do db_alive && break; sleep 1; done
fi
db_alive || { tail -50 "$MYSQL_LOG"; exit 1; }

# Provision.
mysql -u"$MYSQL_ROOT_USER" <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

mysql -u"$MYSQL_ROOT_USER" -e "SHOW DATABASES;"