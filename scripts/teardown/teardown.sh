#!/usr/bin/env bash
#
# honeypot-server 완전 종료 정리 스크립트 (서버에서 직접 실행).
#
# 삭제 대상:
#   1. honeypot 컨테이너(app, seaweedfs) + SeaweedFS 볼륨 + 로컬 빌드 이미지
#   2. 공유 MySQL 의 honeypot DB / 전용 user  (다른 서비스의 DB 는 건드리지 않는다)
#   3. nginx kimtaeeun.conf 의 honeypot 리버스 프록시 location(마커 블록)
#   4. 전송된 로컬 배포 디렉터리(~/deploy/honeypot)
#
# 파괴적이므로 기본은 확인 프롬프트. --yes 로 건너뛸 수 있다.
#
# 사용 예:
#   MYSQL_ROOT_PASSWORD='****' ./teardown.sh
#   MYSQL_ROOT_PASSWORD='****' ./teardown.sh --yes
#
set -euo pipefail
export PATH=/opt/homebrew/bin:/usr/local/bin:$PATH

TARGET_PATH="${TARGET_PATH:-$HOME/deploy/honeypot}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql}"
DB_NAME="${DB_NAME:-honeypot}"
DB_USER="${DB_USER:-honeypot}"
NGINX_CONF="${NGINX_CONF:-/opt/homebrew/etc/nginx/servers/kimtaeeun.conf}"
COMPOSE_FILE="docker-compose.deploy.yml"
APP_IMAGE="honeypot-server-app:latest"
MARKER_BEGIN="# >>> honeypot >>>"
MARKER_END="# <<< honeypot <<<"

ASSUME_YES="false"
[ "${1:-}" = "--yes" ] && ASSUME_YES="true"

echo "================ honeypot teardown ================"
echo " 컨테이너/볼륨/이미지 삭제: honeypot-app, honeypot-seaweedfs, $APP_IMAGE"
echo " MySQL DROP            : DATABASE $DB_NAME, USER '$DB_USER'@'%'  (컨테이너: $MYSQL_CONTAINER)"
echo " nginx 원복            : $NGINX_CONF 의 honeypot 마커 블록 제거"
echo " 로컬 디렉터리 삭제    : $TARGET_PATH"
echo "=================================================="

if [ "$ASSUME_YES" != "true" ]; then
  printf "정말 진행할까요? 'yes' 입력: "
  read -r ans
  [ "$ans" = "yes" ] || { echo "취소됨."; exit 1; }
fi

# 1) 컨테이너 + 볼륨 + 로컬 이미지 제거
if [ -f "$TARGET_PATH/$COMPOSE_FILE" ]; then
  ( cd "$TARGET_PATH" && docker compose -f "$COMPOSE_FILE" down -v --rmi local --remove-orphans ) || true
else
  echo "[teardown] compose 파일 없음 — 컨테이너/볼륨 개별 정리"
  docker rm -f honeypot-app honeypot-seaweedfs 2>/dev/null || true
  docker volume rm honeypot-seaweedfs-data 2>/dev/null || true
fi
docker rmi "$APP_IMAGE" 2>/dev/null || true
docker network rm honeypot-net 2>/dev/null || true
echo "[teardown] containers/volumes/image removed"

# 2) 공유 MySQL 의 honeypot DB / user DROP
if [ -z "${MYSQL_ROOT_PASSWORD:-}" ]; then
  echo "[teardown] WARN: MYSQL_ROOT_PASSWORD 미설정 — DB/user DROP 건너뜀" >&2
elif ! docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER"; then
  echo "[teardown] WARN: MySQL 컨테이너($MYSQL_CONTAINER) 미실행 — DB/user DROP 건너뜀" >&2
else
  docker exec -i "$MYSQL_CONTAINER" mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL || echo "[teardown] WARN: DROP 실패" >&2
DROP DATABASE IF EXISTS \`${DB_NAME}\`;
DROP USER IF EXISTS '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL
  echo "[teardown] dropped MySQL database '$DB_NAME' and user '$DB_USER'"
fi

# 3) nginx honeypot 마커 블록 제거 + reload
if [ -f "$NGINX_CONF" ] && grep -qF "$MARKER_BEGIN" "$NGINX_CONF"; then
  cp "$NGINX_CONF" "${NGINX_CONF}.bak.$(date +%s)"
  TMP="$(mktemp)"
  # 마커 BEGIN ~ END 블록 삭제
  awk -v b="$MARKER_BEGIN" -v e="$MARKER_END" '
    index($0, b) { skip=1 }
    skip && index($0, e) { skip=0; next }
    skip { next }
    { print }
  ' "$NGINX_CONF" > "$TMP"
  mv "$TMP" "$NGINX_CONF"
  if nginx -t 2>/dev/null; then
    nginx -s reload
    echo "[teardown] nginx honeypot locations removed and reloaded"
  else
    echo "[teardown] WARN: nginx -t 실패 — $NGINX_CONF 수동 확인 필요" >&2
  fi
else
  echo "[teardown] nginx honeypot 블록 없음 — 건너뜀"
fi

# 4) 로컬 배포 디렉터리 삭제
if [ -d "$TARGET_PATH" ]; then
  rm -rf "$TARGET_PATH"
  echo "[teardown] removed local deploy dir: $TARGET_PATH"
fi

echo "================ teardown 완료 ===================="
echo "참고: GitHub Secrets 와 DNS(kimtaeeun.site) 는 수동 정리 대상입니다."
