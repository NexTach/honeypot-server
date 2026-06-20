#!/usr/bin/env bash
set -euo pipefail
export PATH=/opt/homebrew/bin:/usr/local/bin:$PATH

# kimtaeeun.site server 블록에 honeypot 리버스 프록시 location 을 idempotent 하게 추가한다.
#  - /honeypot/      -> 백엔드 앱(10103), prefix strip
#  - /honeypot-gifs/ -> SeaweedFS S3(8333), prefix 보존(presigned 서명 path 일치)
# teardown 스크립트가 동일 마커로 이 블록을 제거한다.

NGINX_CONF="${NGINX_CONF:-/opt/homebrew/etc/nginx/servers/kimtaeeun.conf}"
MARKER_BEGIN="# >>> honeypot >>>"
MARKER_END="# <<< honeypot <<<"

if [ ! -f "$NGINX_CONF" ]; then
  echo "[nginx_apply] WARN: $NGINX_CONF not found — skip nginx wiring" >&2
  exit 0
fi

if grep -qF "$MARKER_BEGIN" "$NGINX_CONF"; then
  echo "[nginx_apply] honeypot block already present — skip"
  exit 0
fi

BACKUP="${NGINX_CONF}.bak.$(date +%s)"
cp "$NGINX_CONF" "$BACKUP"
echo "[nginx_apply] backed up to $BACKUP"

BLOCK_FILE="$(mktemp)"
cat > "$BLOCK_FILE" <<'EOF'

    # >>> honeypot >>>
    # managed by honeypot deploy — removed by scripts/teardown/teardown.sh
    location /honeypot/ {
        client_max_body_size 60M;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
        proxy_pass         http://127.0.0.1:10103/;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
    location /honeypot-gifs/ {
        client_max_body_size 60M;
        proxy_pass         http://127.0.0.1:8333;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
    # <<< honeypot <<<
EOF

# 마지막 server 블록 닫는 중괄호(컬럼0의 `}`) 앞에 블록 삽입.
LAST_BRACE="$(grep -n '^}' "$NGINX_CONF" | tail -1 | cut -d: -f1)"
if [ -z "$LAST_BRACE" ]; then
  echo "[nginx_apply] ERROR: closing brace not found in $NGINX_CONF" >&2
  rm -f "$BLOCK_FILE"
  exit 1
fi

TMP="$(mktemp)"
head -n "$((LAST_BRACE - 1))" "$NGINX_CONF" > "$TMP"
cat "$BLOCK_FILE" >> "$TMP"
tail -n "+${LAST_BRACE}" "$NGINX_CONF" >> "$TMP"
mv "$TMP" "$NGINX_CONF"
rm -f "$BLOCK_FILE"

if nginx -t 2>/dev/null; then
  nginx -s reload
  echo "[nginx_apply] nginx reloaded with honeypot locations"
else
  echo "[nginx_apply] ERROR: nginx -t failed — restoring backup" >&2
  cp "$BACKUP" "$NGINX_CONF"
  nginx -t && nginx -s reload || true
  exit 1
fi
