#!/usr/bin/env bash
# Cài JDK 21, MySQL server, git, unzip, curl, jq. Yêu cầu root.
set -euo pipefail
source "$(dirname "$0")/_common.sh"

LOG=${LOG:-/tmp/install.log}; : > "$LOG"
fail() { echo "FAILED at: $1"; tail -40 "$LOG"; exit 1; }

# Không cho apt khởi động service (container không systemd).
if [ ! -f /usr/sbin/policy-rc.d ]; then
  printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d
  chmod +x /usr/sbin/policy-rc.d
fi

echo "[1/2] apt update"
apt-get update -qq >>"$LOG" 2>&1 || fail "apt update"

echo "[2/2] install openjdk-21, mysql-server, tools"
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  -o Dpkg::Use-Pty=0 \
  openjdk-21-jdk-headless mysql-server git unzip curl jq >>"$LOG" 2>&1 \
  || fail "apt install"

echo "--- Verify ---"
java -version 2>&1 | head -1
mysqld --version
ls -d /usr/lib/jvm/java-21* 2>/dev/null || true
echo "(full log: $LOG)"