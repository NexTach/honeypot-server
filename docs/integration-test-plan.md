# 통합 테스트 계획 (curl 기반)

> 로컬 docker-compose 인프라 + bootRun 위에서 전체 API를 curl로 검증한다.
> DataGSM OAuth는 외부 의존이므로 로그인 흐름 대신 **DB에 테스트 유저를 직접 삽입하고 자체 JWT를 발급**해 보호 엔드포인트를 호출한다.

## 사전 확인 (2026-06-19 1차 점검에서 검증 완료)

- ✅ 인프라 기동: `docker compose up -d` → MySQL/SeaweedFS, `ddl-auto=update`로 6개 테이블 생성(users/gifs/tags/gif_tags/likes/reports).
- ✅ 앱 기동: `./gradlew bootRun` → `Started ... on port 8080`, `/v3/api-docs` 200.
  - **해결됨**: springdoc 2.8.x(the-sdk 전이)가 Spring Boot 4.1과 비호환(`NoClassDefFoundError: WebMvcProperties`) → `build.gradle.kts`에서 `springdoc-openapi-starter-webmvc-ui:3.0.3` 명시 오버라이드.
- ✅ 인증: 자체 JWT로 `GET /v1/users/me` 200(CommonApiResponse 래핑), 무토큰 → 403.

## 환경 셋업 순서

1. `docker compose up -d` → `docker inspect -f '{{.State.Health.Status}}' honeypot-mysql`가 `healthy` 될 때까지 대기.
2. S3 버킷 생성(1회): `curl -X PUT http://localhost:8333/honeypot` (이미 있으면 409).
3. `./gradlew bootRun` → 로그에 `Started HoneypotServerApplication` 확인.
4. 테스트 유저 삽입(SQL): `users`에 id=1(GENERAL), id=2(ADMIN). `role`은 STRING enum, `created_at/updated_at` NOT NULL.
5. JWT 발급(HS256, 기본 시크릿 `honeypot.jwt.secret`): 클레임 `sub=userId`, `role`, `iat`, `exp`. 필터는 서명+exp만 검증.

```bash
SECRET='changeThisToAVeryLongSecretKeyForHMACSHA256AtLeast256BitsLong'
b64url(){ openssl base64 -A | tr '+/' '-_' | tr -d '='; }
mkjwt(){ now=$(date +%s); exp=$((now+3600))
  h=$(printf '{"alg":"HS256"}' | b64url)
  p=$(printf '{"sub":"%s","role":"%s","iat":%s,"exp":%s}' "$1" "$2" "$now" "$exp" | b64url)
  s=$(printf '%s.%s' "$h" "$p" | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)
  printf '%s.%s.%s' "$h" "$p" "$s"; }
# mkjwt 1 GENERAL  /  mkjwt 2 ADMIN
```

유효한 1x1 GIF(매직바이트 검증 통과): `printf 'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7' | openssl base64 -d -A > test.gif`

## 🔴 먼저 조사할 미해결 항목

- **GIF 업로드(`POST /v1/gifs`) 응답이 비어서 반환됨** — 유효한 GIF(42B, magic `474946383961`)·정상 토큰인데 응답 본문/`id`가 비어 1차 점검을 중단함. 다음 테스트는 이것부터: HTTP 상태코드(`-w '%{http_code}'`) 확인 + bootRun 로그의 스택트레이스 확인. 의심 지점 — SeaweedFS PutObject(path-style/자격증명), multipart `@RequestPart` 바인딩, 또는 래핑되지 않은 500.

## 엔드포인트 테스트 매트릭스 (다음 회차)

### GIF
- `POST /v1/gifs` 멀티파트 업로드(정상) → 201, id 반환 / 매직바이트 위조 → 400 / 50MB 초과 → 400 / `image/gif` 외 content-type → 400.
- `GET /v1/gifs` 목록·검색: keyword(제목 LIKE / 태그 일치), `sort=latest|popular`, **`sort=오타` → 400**(정리 반영분), 페이지네이션(size 최대 100).
- `GET /v1/gifs/{id}` 상세: 가시성(공개/비공개 소유자/blinded → 404).
- `GET /v1/gifs/{id}/raw` → 302 + presigned `Location`, 리다이렉트 추적 시 바이트 다운로드.
- `PATCH /v1/gifs/{id}` 소유자 메타 수정(title/description/tags/isPublic) / 비소유자 → 403.
- `DELETE /v1/gifs/{id}` 소유자 hard delete → 연관 like/report/gif_tags + S3 객체 삭제 확인.
- `POST /v1/gifs/{id}/share` → shareCount +1, 갱신값 반환.

### Like
- `POST /v1/gifs/{id}/like` 추천(멱등 — 중복 호출해도 likeCount 1) / `DELETE` 해제(-1) / `GET /v1/users/me/likes` 페이지네이션.

### MyPage / Tag
- `GET /v1/users/me/gifs` 내 GIF(비공개·blinded 포함, 상태 표기).
- `GET /v1/tags?keyword=` 자동완성.

### Report (관리자)
- `POST /v1/gifs/{id}/reports`: 타인 GIF 신고(admin이 user1 GIF 신고) → 201 / **본인 GIF 신고 → 400** / **같은 신고자-GIF PENDING 중복 → 409**.
- `GET /v1/admin/reports?status=&page=` (ADMIN) / 비관리자 → 403.
- `PATCH /v1/admin/reports/{id}` `{action}`:
  - `NO_ISSUE` → report status만 변경.
  - `BLIND` → `blindedByAdmin=true`(검색/상세/raw에서 숨김, 소유자 상세 404, 마이페이지엔 표기), report `BLINDED`.
  - `DELETE` → GIF hard delete + 해당 GIF의 모든 신고 cascade 삭제(감사이력 미보존).

### 인증/인가 공통
- 무토큰 → 403, 만료/위조 토큰 → 403, 일반 유저의 `/v1/admin/**` → 403.

## 정리(teardown)
- 앱: `:8080` 리스너 PID `taskkill //PID <pid> //F` + `./gradlew --stop`.
- 인프라: `docker compose down`(named volume 유지 → 유저·데이터 재사용) / 초기화하려면 `docker compose down -v`.
