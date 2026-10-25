#!/usr/bin/env bash
# Khởi động MySQL. MẶC ĐỊNH kill mysqld cũ + wipe datadir (đây là 1 trong 3
# script được phép kill, chỉ kill mysqld).
#
# Options:
#   --force-restart  (default) kill mysqld, wipe datadir, reinit, start
#   --keep           giữ datadir + mysqld hiện có
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

: "${MYSQL_DATA:=/var/lib/mysql}"
: "${MYSQL_RUN:=/var/run/mysqld}"
: "${MYSQL_LOG:=/tmp/mysql.log}"

MODE=force-restart
while [ $# -gt 0 ]; do
  case "$1" in
    --force-restart) MODE=force-restart ;;
    --keep)          MODE=keep ;;
    -h|--help)       sed -n '2,12p' "$0"; exit 0 ;;
    *) die "unknown arg: $1" ;;
  esac; shift
done
log "MODE=$MODE"

if [ "$MODE" = "force-restart" ]; then
  if pgrep -x mysqld >/dev/null; then
    log "Kill mysqld cũ"
    pkill -TERM -x mysqld || true
    for _ in $(seq 1 30); do pgrep -x mysqld >/dev/null || break; sleep 1; done
    if pgrep -x mysqld >/dev/null; then
      log "SIGTERM không đủ → SIGKILL"
      pkill -KILL -x mysqld || true
      sleep 2
    fi
  fi
  log "Wipe datadir: $MYSQL_DATA"
  rm -rf "${MYSQL_DATA:?}"/*
fi

mkdir -p "$MYSQL_RUN"
chown -R mysql:mysql "$MYSQL_RUN" 2>/dev/null || true

if [ ! -d "$MYSQL_DATA/mysql" ]; then
  log "Initialize datadir"
  mysqld --initialize-insecure --user=mysql
fi

if ! db_alive; then
  log "Start mysqld (log: $MYSQL_LOG)"
  setsid nohup mysqld --user=root \
    --bind-address=127.0.0.1 --mysqlx=OFF \
    --max-connections=500 --innodb-buffer-pool-size=2G \
    < /dev/null > "$MYSQL_LOG" 2>&1 &
  for _ in $(seq 1 60); do db_alive && break; sleep 1; done
fi

if ! db_alive; then
  echo "[10_mysql] FAILED: mysqld không phản hồi sau 60s" >&2
  tail -50 "$MYSQL_LOG" >&2 || true
  exit 1
fi

mysql -u"$MYSQL_ROOT_USER" <<SQL
CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

log "MySQL OK"
mysql -u"$MYSQL_ROOT_USER" -e "SHOW DATABASES;"