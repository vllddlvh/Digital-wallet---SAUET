#!/usr/bin/env bash
# Start SUT. ĐƯỢC PHÉP kill SUT cũ theo $APP_PID_FILE (mục đích chính).
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

[ -f "$SUT_DIR_FILE" ] || die "Thiếu $SUT_DIR_FILE. Chạy 05_setup_repo.sh + 20_build_sut.sh."
PROJ=$(cat "$SUT_DIR_FILE")
JAR=$(ls "$PROJ"/build/libs/*.jar 2>/dev/null | grep -v -- '-plain' | head -1)
[ -n "$JAR" ] || die "Không có jar trong $PROJ/build/libs"
log "JAR=$JAR"

# Kill SUT cũ nếu có.
if [ -f "$APP_PID_FILE" ]; then
  OLD=$(cat "$APP_PID_FILE" 2>/dev/null || echo "")
  if [ -n "$OLD" ] && kill -0 "$OLD" 2>/dev/null; then
    log "Kill SUT cũ pid=$OLD"
    kill "$OLD" 2>/dev/null || true
    for _ in $(seq 1 15); do kill -0 "$OLD" 2>/dev/null || break; sleep 1; done
    kill -9 "$OLD" 2>/dev/null || true
  fi
  rm -f "$APP_PID_FILE"
fi

export SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export SERVER_PORT="$APP_PORT"

setsid nohup java -Xms1g -Xmx2g -jar "$JAR" < /dev/null > "$APP_LOG" 2>&1 &
APP_PID=$!
echo "$APP_PID" > "$APP_PID_FILE"
log "SUT pid=$APP_PID, log=$APP_LOG"

READY=0
for i in $(seq 1 90); do
  kill -0 "$APP_PID" 2>/dev/null || {
    echo "[30_start_app] FAILED: SUT died early (pid=$APP_PID)" >&2
    tail -60 "$APP_LOG" >&2 || true
    exit 1
  }
  code=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "localhost:${APP_PORT}/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d '{"username":"x","password":"x"}' 2>/dev/null) || code="000"
  if [ "$code" != "000" ]; then
    log "SUT ready after ~$((i*2))s (probe HTTP $code)"
    READY=1
    break
  fi
  sleep 2
done

if [ "$READY" -ne 1 ]; then
  echo "[30_start_app] FAILED: SUT không ready sau 180s" >&2
  tail -60 "$APP_LOG" >&2 || true
  exit 1
fi

log "OK"
grep -iE "Started|ERROR|Exception" "$APP_LOG" | tail -10 || true