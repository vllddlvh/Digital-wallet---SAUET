#!/usr/bin/env bash
# Build SUT. KHÔNG kill gì.
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

need git; need unzip; need curl

if [ -z "${SUT_DIR:-}" ] || [ ! -d "${SUT_DIR:-}" ]; then
  if [ -f "$SUT_DIR_FILE" ]; then
    SUT_DIR=$(cat "$SUT_DIR_FILE")
    log "SUT_DIR từ $SUT_DIR_FILE = $SUT_DIR"
  fi
fi
[ -d "$SUT_DIR" ] || die "SUT_DIR không tồn tại: ${SUT_DIR:-<unset>}. Chạy 05_setup_repo.sh trước."

PROJ=""
while IFS= read -r f; do
  if grep -q "org.springframework.boot" "$f"; then
    PROJ=$(dirname "$f"); break
  fi
done < <(find "$SUT_DIR" -maxdepth 4 -name 'build.gradle*' 2>/dev/null | sort)
[ -n "$PROJ" ] || die "Không tìm thấy module Spring Boot trong $SUT_DIR"

log "PROJECT DIR = $PROJ"
echo "$PROJ" > "$SUT_DIR_FILE"

cd "$PROJ"
if [ -f ./gradlew ]; then
  chmod +x ./gradlew
  GRADLE=./gradlew
else
  log "Không có gradlew → tải gradle"
  curl -sSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
  unzip -q -o /tmp/gradle.zip -d /opt
  GRADLE=/opt/gradle-8.14.3/bin/gradle
fi

"$GRADLE" bootJar -x test --no-daemon --console=plain
log "Build SUT OK"
ls -lh build/libs/