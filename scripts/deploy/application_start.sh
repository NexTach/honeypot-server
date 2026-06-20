#!/usr/bin/env bash
set -euo pipefail
export PATH=/opt/homebrew/bin:/usr/local/bin:$PATH

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

# honeypot 앱 + SeaweedFS 기동.
docker compose -f docker-compose.deploy.yml up -d --build
echo "[application_start] honeypot compose up (app on host port 10103)"

# SeaweedFS S3 버킷 보장(이미 있으면 무시).
S3_BUCKET="$(grep '^S3_BUCKET=' app.env | cut -d= -f2-)"
if [ -n "$S3_BUCKET" ]; then
  for i in $(seq 1 20); do
    if docker exec honeypot-seaweedfs sh -c "echo 's3.bucket.create -name ${S3_BUCKET}' | weed shell" >/dev/null 2>&1; then
      echo "[application_start] ensured S3 bucket: ${S3_BUCKET}"
      break
    fi
    sleep 2
  done
fi

# nginx 리버스 프록시 location 적용(idempotent).
"$DEPLOY_DIR/nginx_apply.sh"
