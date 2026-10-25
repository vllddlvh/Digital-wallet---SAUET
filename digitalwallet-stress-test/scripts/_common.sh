#!/usr/bin/env bash
# Default chung cho các script. Source: `source "$(dirname "$0")/_common.sh"`.
# Tất cả biến có thể override qua env trước khi gọi script.

# ── SUT ──────────────────────────────────────────────────────────────────────
: "${SUT_REPO:=https://github.com/vllddlvh/SAUETDigitalWallet.git}"
: "${SUT_DIR:=/tmp/SAUETDigitalWallet}"
: "${SUT_DIR_FILE:=/tmp/sut_dir}"

# ── App ──────────────────────────────────────────────────────────────────────
: "${APP_PORT:=8080}"
: "${APP_LOG:=/tmp/app.log}"
: "${APP_PID_FILE:=/tmp/app.pid}"

# ── DB ───────────────────────────────────────────────────────────────────────
: "${DB_NAME:=digitalwallet_db}"
: "${DB_USER:=wallet}"
: "${DB_PASS:=wallet}"
: "${DB_HOST:=127.0.0.1}"
: "${DB_PORT:=3306}"
: "${MYSQL_ROOT_USER:=root}"

# ── Java ─────────────────────────────────────────────────────────────────────
: "${JAVA_HOME:=/usr/lib/jvm/java-21-openjdk-amd64}"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# ── Output ───────────────────────────────────────────────────────────────────
: "${RESULTS_DIR:=./results}"

# ── Helpers ──────────────────────────────────────────────────────────────────
log()  { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*" >&2; }
die()  { log "ERROR: $*"; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "missing tool: $1"; }

# Trap ERR: echo file + line + command trước khi exit (yêu cầu #2).
# Cách dùng: source _common.sh; trap_errors; (ngay sau `set -euo pipefail`)
trap_errors() {
  trap 'rc=$?; echo "[$(basename "$0")] ERROR line $LINENO (exit=$rc): $BASH_COMMAND" >&2' ERR
}

db_alive() { mysqladmin ping --silent 2>/dev/null; }

table_exists() {
  mysql -u"$MYSQL_ROOT_USER" -N -B -e \
    "SELECT 1 FROM information_schema.tables WHERE table_schema='$DB_NAME' AND table_name='$1' LIMIT 1" \
    2>/dev/null | grep -q 1
}