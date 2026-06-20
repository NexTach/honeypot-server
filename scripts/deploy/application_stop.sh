#!/usr/bin/env bash
set -euo pipefail
export PATH=/opt/homebrew/bin:/usr/local/bin:$PATH

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

# 기존 honeypot 컨테이너 정리(볼륨은 보존). 없어도 무시.
docker compose -f docker-compose.deploy.yml down --remove-orphans 2>/dev/null || true
echo "[application_stop] stopped honeypot compose (if any)"
