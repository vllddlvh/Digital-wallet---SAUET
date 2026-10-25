#!/usr/bin/env bash
# Khởi động SUT jar nền. Chờ tới khi cổng HTTP trả lời.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

[ -f "$SUT_DIR_FILE" ] || die "Missing $SUT_DIR_FILE; run 20_build_sut.sh first"
PROJ=$(cat "$SUT_DIR_FILE")
JAR=$(ls "$PROJ"/build/libs/*.jar 2>/dev/null | grep -v -- '-plain' | head -1)
[ -n "$JAR" ] || die "No jar in $PROJ/build/libs"
log "JAR=$JAR"

# Kill instance cũ.
if [ -f "$APP_PID_FILE" ]; then
  kill "$(cat "$APP_PID_FILE")" 2>/dev/null || true
  sleep 2
fi

export SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
export SPRING_JPA_HIBERNATE_DDL_AUTO=update
export SERVER_PORT="$APP_PORT"

setsid nohup java -Xms1g -Xmx2g -jar "$JAR" < /dev/null > "$APP_LOG" 2>&1 &
APP_PID=$!; echo "$APP_PID" > "$APP_PID_FILE"

for i in $(seq 1 90); do
  kill -0 "$APP_PID" 2>/dev/null || { echo "APP DIED"; tail -60 "$APP_LOG"; exit 1; }
  code=$(curl -s -o /dev/null -w '%{http_code}' -X POST "localhost:${APP_PORT}/api/v1/auth/login" \
         -H 'Content-Type: application/json' -d '{"username":"x","password":"x"}')
  [ "$code" != "000" ] && { log "UP after ~$((i*2))s, login probe -> HTTP $code"; break; }
  sleep 2
done
grep -iE "Started|ERROR|Exception" "$APP_LOG" | tail -10 || true