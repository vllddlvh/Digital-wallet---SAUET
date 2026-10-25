#!/usr/bin/env bash
# Cài JDK 21, MySQL server, git, unzip, curl, jq. Yêu cầu root. KHÔNG kill gì.
set -euo pipefail
source "$(dirname "$0")/_common.sh"
trap_errors

LOG=${LOG:-/tmp/install.log}; : > "$LOG"
fail() { echo "[00_install] FAILED: $1" >&2; tail -40 "$LOG" >&2; exit 1; }

if [ ! -f /usr/sbin/policy-rc.d ]; then
  printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d
  chmod +x /usr/sbin/policy-rc.d
fi

echo "[1/2] apt update"
apt-get update -qq >>"$LOG" 2>&1 || fail "apt update"

echo "[2/2] install packages"
DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  -o Dpkg::Use-Pty=0 \
  openjdk-21-jdk-headless mysql-server git unzip curl jq >>"$LOG" 2>&1 \
  || fail "apt install"

echo "[3/3] k6 (pin v0.55.0)"
K6_VERSION=v0.55.0
if ! command -v k6 >/dev/null 2>&1 \
   || [ "$(k6 version 2>/dev/null | awk '{print $2}')" != "${K6_VERSION}" ]; then
  curl -sSL "https://github.com/grafana/k6/releases/download/${K6_VERSION}/k6-${K6_VERSION}-linux-amd64.tar.gz" \
    -o /tmp/k6.tar.gz >>"$LOG" 2>&1 || fail "k6 download"
  tar -xzf /tmp/k6.tar.gz -C /tmp >>"$LOG" 2>&1 || fail "k6 extract"
  install -m 0755 "/tmp/k6-${K6_VERSION}-linux-amd64/k6" /usr/local/bin/k6 \
    || fail "k6 install"
fi

echo "--- Verify ---"
java -version 2>&1 | head -1
mysqld --version
k6 version
ls -d /usr/lib/jvm/java-21* 2>/dev/null || true
echo "(full log: $LOG)"