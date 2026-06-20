#!/usr/bin/env bash
set -euo pipefail
export PATH=/opt/homebrew/bin:/usr/local/bin:$PATH

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

if [ ! -f app.env ]; then
  echo "[after_install] ERROR: app.env not found in $DEPLOY_DIR" >&2
  exit 1
fi
if [ ! -f app.jar ]; then
  echo "[after_install] ERROR: app.jar not found in $DEPLOY_DIR" >&2
  exit 1
fi

# SeaweedFS S3 identity(s3.json) 를 app.env 의 S3 자격증명으로 생성.
S3_ACCESS_KEY="$(grep '^S3_ACCESS_KEY=' app.env | cut -d= -f2-)"
S3_SECRET_KEY="$(grep '^S3_SECRET_KEY=' app.env | cut -d= -f2-)"
if [ -z "$S3_ACCESS_KEY" ] || [ -z "$S3_SECRET_KEY" ]; then
  echo "[after_install] ERROR: S3_ACCESS_KEY/S3_SECRET_KEY missing in app.env" >&2
  exit 1
fi

cat > s3.json <<EOF
{
  "identities": [
    {
      "name": "honeypot",
      "credentials": [
        { "accessKey": "${S3_ACCESS_KEY}", "secretKey": "${S3_SECRET_KEY}" }
      ],
      "actions": ["Admin", "Read", "Write", "List", "Tagging"]
    }
  ]
}
EOF
echo "[after_install] generated s3.json"

echo "[after_install] building honeypot-server-app image"
docker compose -f docker-compose.deploy.yml build
