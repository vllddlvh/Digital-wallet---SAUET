#!/usr/bin/env bash
# Clone và build SUT. Ghi đường dẫn module vào $SUT_DIR_FILE.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

need git; need unzip; need curl

# 1. Clone (chỉ nếu chưa có).
if [ ! -d "$SUT_DIR" ]; then
  git clone "$SUT_REPO" "$SUT_DIR"
else
  log "Reuse existing SUT_DIR: $SUT_DIR"
fi

# 2. Tìm module có plugin Spring Boot.
PROJ=""
while IFS= read -r f; do
  if grep -q "org.springframework.boot" "$f"; then
    PROJ=$(dirname "$f"); break
  fi
done < <(find "$SUT_DIR" -maxdepth 4 -name 'build.gradle*' 2>/dev/null | sort)
[ -n "$PROJ" ] || die "No Spring Boot module in $SUT_DIR"
echo "$PROJ" > "$SUT_DIR_FILE"
log "PROJECT DIR = $PROJ"

# 3. Build.
cd "$PROJ"
if [ -f ./gradlew ]; then
  chmod +x ./gradlew
  GRADLE=./gradlew
else
  curl -sSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.14.3-bin.zip
  unzip -q -o /tmp/gradle.zip -d /opt
  GRADLE=/opt/gradle-8.14.3/bin/gradle
fi
"$GRADLE" bootJar -x test --no-daemon --console=plain
ls -lh build/libs/