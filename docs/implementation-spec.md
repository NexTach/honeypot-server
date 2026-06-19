# Honeypot 서버 구현 명세서

> GSM 학생 대상 GIF 공유/검색 플랫폼 백엔드. 본 문서는 `/planning` 인터뷰로 확정된 설계 결정을 종합한 구현 명세서다.
> 작성일: 2026-06-19

---

## 1. 개요 & 기술 스택

| 항목 | 값 |
|------|-----|
| 언어 | Kotlin 2.3.21 |
| 프레임워크 | Spring Boot 4.1.0 (Spring **WebMVC**, 서블릿 기반) |
| JVM | Java 25 toolchain |
| 영속성 | Spring Data JPA + MySQL (`ddl-auto=update`) |
| 보안 | Spring Security + 자체 JWT |
| 객체 스토리지 | SeaweedFS (S3 호환), AWS SDK v2 로 접근 |
| 인증 제공자 | DataGSM OAuth (Authorization Code + PKCE) |
| 공통 SDK | `the-sdk` (응답 래핑/예외/로깅/Swagger), `datagsm-oauth-sdk-java` |
| 베이스 패키지 | `team.themoment.honeypotserver` |
| API 베이스 경로 | `/v1` |

---

## 2. 확정된 핵심 결정 요약 (Decision Log)

| # | 영역 | 결정 |
|---|------|------|
| 1 | 아키텍처 | 계층형 + 도메인별 패키지 (`presentation`/`application`/`domain`/`infra` + `global`) |
| 2 | 파일 스토리지 | SeaweedFS (S3 호환), docker-compose 통합 |
| 3 | 인증 흐름 | 백엔드가 code 교환 → userinfo 조회 → 유저 프로비저닝 → **자체 JWT** 발급 |
| 4 | JWT 전달 | access = 응답 본문(Bearer 헤더), refresh = HttpOnly+Secure 쿠키 |
| 5 | JWT refresh | Stateless + rotation, 로그아웃 = 쿠키 삭제 (서버 저장 안 함) |
| 6 | Role 부여 | 기본 `GENERAL`, `ADMIN`은 직접 DB 수정으로 승격 |
| 7 | User 필드 | `oauthAccountId`(unique) + `name` + `studentNumber` + `role` + `email`, 로그인마다 name/studentNumber 재동기화 |
| 8 | 비학생 계정 | 로그인 거부(403) — 학생 전용 서비스 |
| 9 | 태그 모델 | `Tag` 엔티티 + `gif_tags` ManyToMany |
| 10 | 검색 | Offset 기반 `Pageable`, 단일 `keyword`로 제목+태그 통합 매칭 |
| 11 | 카운터 | GIF 행에 비정규화 `likeCount`/`shareCount` + 원자적 증감 |
| 12 | 추천 | 별도 `Like` 엔티티 (user+gif unique) — 중복방지 + 마이페이지 |
| 13 | 공유수 | 복사 액션마다 무조건 +1 (dedup 없음, 별도 로그 테이블 없음) |
| 14 | GIF 상태 | 단일 `isPublic` + `blindedByAdmin` Boolean, **hard delete** |
| 15 | 신고-삭제 | GIF hard delete 시 연관 신고/추천/태그 **cascade 삭제**, 신고는 독립 처리 |
| 16 | 미디어 서빙 | 백엔드 가시성 인가 후 SeaweedFS **단기 presigned URL로 302 리다이렉트** |
| 17 | 썸네일 | 생성 안 함, 원본 GIF만 사용 |
| 18 | API 컨벤션 | `the-sdk` 사용 (`CommonApiResponse` 자동 래핑, `ExpectedException` + 자동 `GlobalExceptionHandler`, SpringDoc Swagger) |
| 19 | 스키마 관리 | JPA `ddl-auto=update` (Flyway 미사용) |
| 20 | 테스트 | 당장 없음 |
| 21 | 업로드 제한 | 엄격 검증(매직바이트 + content-type), 최대 크기 env 관리(기본 50MB) |
| 22 | 프론트 토폴로지 | 별도 오리진 SPA → CORS allowCredentials, 쿠키 `SameSite=None; Secure` |

---

## 3. 아키텍처 & 패키지 구조

도메인별 패키지 안에 4계층을 둔다. 공통 인프라는 `global`에 모은다.

```
team.themoment.honeypotserver
├─ HoneypotServerApplication.kt
├─ global
│  ├─ config        # SecurityConfig, CorsConfig, S3Config(SeaweedFS), DataGsmConfig, JacksonConfig
│  ├─ security       # JwtProvider, JwtAuthenticationFilter, AuthPrincipal, @CurrentUser ArgumentResolver
│  └─ common         # 공통 상수/유틸 (응답·예외·로깅·Swagger 는 the-sdk 가 처리하므로 직접 구현 X)
├─ user
│  ├─ presentation   # AuthController, UserController + request/response DTO
│  ├─ application    # AuthService, UserService
│  ├─ domain         # User, Role(enum)
│  └─ infra          # UserRepository, DataGsmClient 래퍼
├─ gif
│  ├─ presentation   # GifController + DTO
│  ├─ application    # GifCommandService, GifQueryService, GifMediaService
│  ├─ domain         # Gif, GifRepository(인터페이스)
│  └─ infra          # GifJpaRepository, GifQueryRepositoryImpl, GifStorageAdapter(S3)
├─ tag
│  ├─ application    # TagService (find-or-create, 정규화)
│  ├─ domain         # Tag
│  └─ infra          # TagRepository
├─ like
│  ├─ presentation   # LikeController
│  ├─ application    # LikeService
│  ├─ domain         # Like
│  └─ infra          # LikeRepository
└─ report
   ├─ presentation   # ReportController, AdminReportController
   ├─ application    # ReportService
   ├─ domain         # Report, ReportStatus(enum), ReportAction(enum)
   └─ infra          # ReportRepository
```

규칙:
- `domain`: JPA 엔티티 + enum + 도메인 규칙. `application`: 서비스(트랜잭션 경계) + command/result. `infra`: Spring Data 리포지토리, 외부 어댑터(S3/DataGSM). `presentation`: 컨트롤러 + request/response DTO.
- 컨트롤러는 raw DTO 를 반환한다(`the-sdk` 가 `CommonApiResponse` 로 자동 래핑). 예외는 `ExpectedException` 을 throw 한다.

---

## 4. 의존성 (build.gradle.kts 추가분)

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }   // the-sdk, datagsm-oauth-sdk
}

dependencies {
    // 기존 유지: data-jpa, security, webmvc, kotlin-reflect, jackson-module-kotlin, mysql-connector-j

    implementation("com.github.themoment-team:the-sdk:1.4")
    implementation("com.github.themoment-team:datagsm-oauth-sdk-java:1.5.0")

    // S3 호환 (SeaweedFS) 접근 + presigned URL
    implementation(platform("software.amazon.awssdk:bom:2.+"))
    implementation("software.amazon.awssdk:s3")

    // 자체 JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.+")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.+")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.+")

    // 요청 검증
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

> 참고: SpringDoc(Swagger)은 `the-sdk` 가 제공한다. 전이 의존성으로 들어오지 않으면 `springdoc-openapi-starter-webmvc-ui` 를 명시적으로 추가한다.

---

## 5. 도메인 모델 & 스키마

`ddl-auto=update` 로 엔티티에서 스키마를 생성한다. 인덱스/유니크 제약은 JPA 애너테이션으로 명시한다.

### 5.1 User
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK, auto |
| oauthAccountId | Long | **unique**, not null (= DataGSM `UserInfo.id`) |
| name | String | not null (로그인마다 재동기화) |
| studentNumber | Int | not null (로그인마다 재동기화) |
| email | String | nullable |
| role | enum `Role { GENERAL, ADMIN }` | not null, default `GENERAL` |
| createdAt / updatedAt | Instant | 감사 |

- 안정 식별 키는 `oauthAccountId`. `name`/`studentNumber` 는 매 로그인 시 DataGSM 값으로 갱신(학년 진급으로 학번이 바뀌므로).
- `ADMIN` 승격은 운영자가 직접 DB `UPDATE` (별도 승격 API/부트스트랩 없음).

### 5.2 Gif
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| title | String(100) | not null |
| description | TEXT | nullable |
| isPublic | Boolean | not null, default `true` (소유자 공개여부) |
| blindedByAdmin | Boolean | not null, default `false` (관리자 비공개처리, 소유자가 못 되돌림) |
| objectKey | String | not null (SeaweedFS 객체 키) |
| contentType | String | `image/gif` |
| fileSize | Long | bytes |
| width / height | Int | nullable (선택적 메타데이터) |
| likeCount | Long | not null, default 0 (비정규화) |
| shareCount | Long | not null, default 0 (비정규화) |
| uploader | ManyToOne&lt;User&gt; | not null |
| tags | ManyToMany&lt;Tag&gt; | `gif_tags` 조인 |
| createdAt / updatedAt | Instant | |

- 인덱스: `created_at`(최신순), `like_count`(인기순), `uploader_id`.
- **hard delete**: 삭제 시 `gif_tags`, `likes`, `reports` 를 cascade 제거(`@OnDelete` 또는 서비스에서 명시적 삭제).

### 5.3 Tag
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| name | String | **unique**, not null |

- 정규화 규칙: 입력값 `trim` + 소문자화 후 저장. 업로드/수정 시 `find-or-create`.
- `gif_tags(gif_id, tag_id)` 복합 PK 조인 테이블.

### 5.4 Like
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| user | ManyToOne&lt;User&gt; | not null |
| gif | ManyToOne&lt;Gif&gt; | not null |
| createdAt | Instant | |

- **unique 제약 `(user_id, gif_id)`** — 중복 추천 방지.
- 추천/해제 시 `Gif.likeCount` 를 원자적 UPDATE(`SET like_count = like_count + 1` / `- 1`).

### 5.5 Report
| 필드 | 타입 | 제약 |
|------|------|------|
| id | Long | PK |
| reporter | ManyToOne&lt;User&gt; | not null |
| gif | ManyToOne&lt;Gif&gt; | not null (gif hard delete 시 cascade) |
| reasonTitle | String(100) | not null (신고사유/제목) |
| detail | TEXT | not null (신고내용/상세) |
| status | enum `ReportStatus` | not null, default `PENDING` |
| processedBy | ManyToOne&lt;User&gt; | nullable (처리한 관리자) |
| createdAt / processedAt | Instant | processedAt nullable |

- `ReportStatus { PENDING, NO_ISSUE, BLINDED, DELETED }` — 요구사항의 미처리/문제없음/비공개처리/삭제.
- **중요한 귀결**: `DELETE` 처리는 GIF 를 hard delete 하므로 그 GIF 의 모든 신고(처리 중인 것 포함)가 cascade 로 사라진다. 따라서 `DELETED` 상태는 영속되지 않으며, **삭제된 GIF 의 신고 감사 이력은 남지 않는다.**
- 인덱스: `status`(관리자 목록 필터).

---

## 6. 인증 / 인가

### 6.1 DataGSM OAuth (백엔드 주도, PKCE)

- SDK: `DataGsmOAuthClient` 를 Bean 으로 등록. 설정은 env (`DATAGSM_CLIENT_ID`, `DATAGSM_CLIENT_SECRET`, base URL).
- **로그인 시작** `GET /v1/auth/datagsm/login`:
  1. `state` + PKCE `codeVerifier` 생성.
  2. `state`/`codeVerifier` 를 **단기 HttpOnly 쿠키**(서명/짧은 TTL)에 저장 — 백엔드 stateless 유지.
  3. `client.createAuthorizationUrl(redirectUri).enablePkce(verifier, "S256").state(state).scope("self:read").build()` 로 302 리다이렉트.
- **콜백** `GET /v1/auth/datagsm/callback?code&state`:
  1. 쿠키의 `state` 와 일치 검증.
  2. `exchangeCodeForToken(code, redirectUri, codeVerifier)` → `getUserInfo(accessToken)`.
  3. `userInfo.isStudent == false` → **403 거부**.
  4. 유저 프로비저닝: `oauthAccountId = userInfo.id` 로 조회, 없으면 생성. `name = student.name`, `studentNumber = student.studentNumber`, `email = userInfo.email` 갱신.
  5. 자체 JWT 발급 → refresh 쿠키 Set-Cookie, **프론트 리다이렉트 URI 로 302** (`#accessToken=...` fragment 로 access token 전달).
- DataGSM access/refresh 토큰은 서버에서만 사용하고 클라이언트에 노출하지 않는다. 프로비저닝 후에는 우리 DB 의 스냅샷으로 동작하므로 DataGSM 재호출은 불필요.

### 6.2 자체 JWT

- 서명: HMAC-SHA256, 시크릿은 env (`JWT_SECRET`).
- 클레임: `sub = userId`, `role`, `iat`, `exp`.
- 수명: **access ~30분, refresh ~14일** (env 로 조정 가능).
- 전달: access = 응답 본문(클라이언트가 메모리 보관 후 `Authorization: Bearer`), refresh = `HttpOnly; Secure; SameSite=None` 쿠키.
- **재발급** `POST /v1/auth/reissue`: refresh 쿠키 검증 → 새 access + **새 refresh(rotation)** 발급.
- **로그아웃** `POST /v1/auth/logout`: refresh 쿠키 만료(`Max-Age=0`). (Stateless 이므로 탈취된 refresh 는 만료까지 무효화 불가 — 알려진 한계.)

### 6.3 Spring Security

- `JwtAuthenticationFilter`(OncePerRequestFilter) 가 `Authorization` 헤더 검증 → `SecurityContext` 에 `AuthPrincipal(userId, role)` 적재.
- `@CurrentUser` ArgumentResolver 로 컨트롤러에서 현재 유저 주입.
- 인가 규칙:
  - 인증 불필요: `/v1/auth/datagsm/**`, `/v1/auth/reissue`, `/swagger-ui/**`, `/v3/api-docs/**`.
  - **그 외 모든 API 는 인증 필요** (학교 내부 서비스 — 익명 브라우징 미지원).
  - `/v1/admin/**` 는 `ROLE_ADMIN` 필요.
- CSRF: 메인 API 는 Authorization 헤더 기반이라 안전. 쿠키 기반 엔드포인트(`/reissue`, `/logout`)는 SameSite=None + CORS(응답 교차출처 read 차단)에 의존하며, 필요 시 double-submit 토큰 보강.
- CORS: `allowedOrigins = ${FRONTEND_ORIGIN}`, `allowCredentials = true`, 노출 헤더에 `Authorization`.

---

## 7. API 명세 (베이스 `/v1`)

> 모든 응답은 `the-sdk` 의 `CommonApiResponse{status, code, message, data}` 로 자동 래핑된다. 아래 "응답"은 `data` 페이로드 기준.

### 7.1 인증/유저
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | `/v1/auth/datagsm/login` | DataGSM 로그인 시작(302) | 공개 |
| GET | `/v1/auth/datagsm/callback` | 콜백, JWT 발급 후 프론트로 302 | 공개 |
| POST | `/v1/auth/reissue` | refresh 쿠키로 access 재발급(rotation) | 쿠키 |
| POST | `/v1/auth/logout` | refresh 쿠키 삭제 | 인증 |
| GET | `/v1/users/me` | 내 프로필 | 인증 |

### 7.2 GIF
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | `/v1/gifs` | 업로드 (multipart: `file` + 메타데이터) | 인증 |
| GET | `/v1/gifs` | 검색/목록 `?keyword=&sort=latest\|popular&page=&size=` | 인증 |
| GET | `/v1/gifs/{id}` | 상세 (가시성 검증) | 인증 |
| GET | `/v1/gifs/{id}/raw` | presigned URL 로 302 (가시성 검증) | 인증 |
| PATCH | `/v1/gifs/{id}` | 메타데이터 수정(title/description/tags/isPublic) — 파일 자체는 수정 불가 | 소유자 |
| DELETE | `/v1/gifs/{id}` | hard delete | 소유자 |
| POST | `/v1/gifs/{id}/share` | shareCount +1, 갱신값 반환 | 인증 |
| GET | `/v1/users/me/gifs` | 내가 올린 GIF(비공개/blinded 포함, 상태 표기) | 인증 |

### 7.3 추천(Like)
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | `/v1/gifs/{id}/like` | 추천(멱등, 없으면 생성) | 인증 |
| DELETE | `/v1/gifs/{id}/like` | 추천 해제 | 인증 |
| GET | `/v1/users/me/likes` | 내가 추천한 GIF 목록(페이지네이션) | 인증 |

### 7.4 신고(Report)
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| POST | `/v1/gifs/{id}/reports` | 신고 작성(reporter = 나) | 인증 |
| GET | `/v1/admin/reports` | 신고 목록 `?status=&page=` | ADMIN |
| PATCH | `/v1/admin/reports/{id}` | 처리: body `{ action: NO_ISSUE \| BLIND \| DELETE }` | ADMIN |

### 7.5 태그(선택)
| 메서드 | 경로 | 설명 | 권한 |
|--------|------|------|------|
| GET | `/v1/tags` | 태그 검색/자동완성 `?keyword=` | 인증 |

---

## 8. GIF 파일 처리

### 8.1 업로드 검증 (엄격)
- content-type 이 `image/gif` 인지 확인.
- **매직바이트 검증**: 파일 헤더가 `GIF87a`(`47 49 46 38 37 61`) 또는 `GIF89a`(`47 49 46 38 39 61`)인지 확인 — 위조 차단.
- 크기 제한: `honeypot.upload.max-size`(env, **기본 50MB**). Spring multipart 한계도 함께 설정.
- 통과 시 SeaweedFS 에 `objectKey`(예: `gifs/{uuid}.gif`)로 업로드, 메타데이터 행 생성.

### 8.2 스토리지 어댑터 (SeaweedFS / S3)
- AWS SDK v2 `S3Client` 를 SeaweedFS S3 엔드포인트로 구성: `endpointOverride`, **path-style access 활성화**, env 자격증명(`S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`).
- 업로드: `PutObject`. 삭제: `DeleteObject`(GIF hard delete 시 호출).

### 8.3 미디어 서빙
- `GET /v1/gifs/{id}/raw`:
  1. 가시성 인가: `blindedByAdmin == false` AND (`isPublic == true` OR `uploader == me`) — 아니면 404 (관리자는 전체 허용).
  2. `S3Presigner` 로 **단기(예: 60초) presigned GET URL** 생성.
  3. 해당 URL 로 **302 리다이렉트** — 바이트는 SeaweedFS 가 직접 전송(백엔드 대역폭 절감).
- 썸네일 없음: 목록/상세 모두 동일 `/raw` 사용.

---

## 9. 검색 / 페이지네이션

- Offset 기반 `Pageable`(`page`, `size`). 기본 size 20, 최대 100.
- 단일 `keyword` 매칭: `title LIKE %keyword%` **OR** `tag.name = keyword`(정규화 일치). keyword 없으면 전체 목록.
- 정렬:
  - `latest`(기본): `created_at DESC, id DESC`.
  - `popular`: `like_count DESC, id DESC`.
- 가시성 필터(목록/검색 공통): `blindedByAdmin = false` AND (`isPublic = true` OR `uploader = me`).
- 무한 스크롤: 클라이언트가 `page` 증가로 호출. (삽입 시 경계 drift 는 학교 규모에서 허용.)
- 구현: 동적 조건 + 조인(태그) 때문에 `GifQueryRepository` 커스텀 구현(Specification 또는 직접 JPQL/Criteria) 권장.

---

## 10. 신고 처리 로직

관리자 `PATCH /v1/admin/reports/{id}` 의 `action` 별 부수효과:

| action | Gif 변경 | Report 변경 |
|--------|----------|-------------|
| `NO_ISSUE` | 없음 | `status=NO_ISSUE`, `processedBy`, `processedAt` |
| `BLIND` | `blindedByAdmin = true` (검색/상세/raw 에서 숨김, 소유자도 못 되돌림) | `status=BLINDED`, `processedBy`, `processedAt` |
| `DELETE` | **hard delete** (gif_tags/likes/reports cascade 제거 + SeaweedFS 객체 삭제) | 행 자체가 cascade 로 삭제됨 |

- 신고는 **독립 처리**: 한 GIF 에 여러 신고가 있어도 자동 일괄 처리하지 않는다. `BLIND`/`DELETE` 후 다른 신고는 관리자가 개별 판단(단, `DELETE` 면 함께 사라짐).
- 작성 시 기본 규칙(기본값, 11.절 참조): 같은 신고자-GIF 의 `PENDING` 중복 신고 차단, 본인 GIF 신고 차단.

---

## 11. 추천 / 공유 카운터

- **추천**: `POST .../like` → `Like` 행 생성(이미 있으면 멱등 무시) + `likeCount` 원자적 +1. `DELETE .../like` → 행 삭제 + `-1`. 동시성은 `UPDATE ... SET like_count = like_count + 1` 원자 연산으로 처리.
- **공유수**: `POST .../share` → `shareCount` 원자적 +1, 갱신값 반환. **dedup 없음**(요구사항: "복사가 진행된 수"). 별도 로그 테이블 없음.
- 공유 "링크"는 프론트엔드 상세 페이지 URL(`{FRONTEND}/gifs/{id}`)을 복사하는 것으로 간주하며, 본 API 는 카운트만 담당한다.

---

## 12. 인프라 / 설정

### 12.1 docker-compose.yml (개발 통합)
```yaml
services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_DATABASE: honeypot
      MYSQL_ROOT_PASSWORD: ...
    ports: ["3306:3306"]
    volumes: [mysql-data:/var/lib/mysql]

  seaweedfs:
    image: chrislusf/seaweedfs:latest
    command: "server -dir=/data -s3 -s3.port=8333"
    ports: ["8333:8333", "9333:9333", "8888:8888"]
    volumes: [seaweed-data:/data]

  # (선택) app 서비스로 백엔드까지 통합 가능

volumes:
  mysql-data:
  seaweed-data:
```
- SeaweedFS 올인원(`server -s3`)으로 단일 컨테이너 S3 게이트웨이 제공. 버킷 생성은 부팅 시 스크립트 또는 앱 기동 시 보장.

### 12.2 application.yml (핵심 키)
```yaml
spring:
  datasource: { url, username, password }   # MySQL
  jpa:
    hibernate.ddl-auto: update
  servlet.multipart:
    max-file-size: 50MB
    max-request-size: 50MB

honeypot:
  upload.max-size: 50MB
  jwt:
    secret: ${JWT_SECRET}
    access-ttl: 30m
    refresh-ttl: 14d
  frontend:
    origin: ${FRONTEND_ORIGIN}
    redirect-uri: ${FRONTEND_REDIRECT_URI}
  s3:
    endpoint: ${S3_ENDPOINT}
    access-key: ${S3_ACCESS_KEY}
    secret-key: ${S3_SECRET_KEY}
    bucket: ${S3_BUCKET}
    presign-ttl: 60s

datagsm.oauth:
  client-id: ${DATAGSM_CLIENT_ID}
  client-secret: ${DATAGSM_CLIENT_SECRET}
  authorization-base-url: https://oauth.authorization.datagsm.kr
  user-info-base-url: https://oauth.resource.datagsm.kr
  redirect-uri: ${DATAGSM_REDIRECT_URI}   # 백엔드 callback

sdk:                      # the-sdk
  swagger.paths-to-match: ["/v1/**"]
```
- 시크릿은 전부 env. `local` 프로필은 docker-compose 의존(MySQL/SeaweedFS), 운영 프로필 분리.

---

## 13. 기본값 / 가정 & 오픈 이슈

인터뷰에서 명시되지 않아 **합리적 기본값으로 확정**한 항목 (필요 시 조정):

1. **모든 API 인증 필요** — 공개 GIF 도 익명 브라우징 불가(학교 내부 서비스). 익명 열람을 원하면 목록/상세/raw 를 공개로 전환.
2. **신고 중복/자기신고**: 같은 신고자-GIF 의 `PENDING` 중복 신고는 409 차단, 본인 GIF 신고는 400 차단.
3. **신고 사유**: `reasonTitle`/`detail` 모두 자유 텍스트(사전 정의 카테고리 없음).
4. **JWT TTL**: access 30분 / refresh 14일.
5. **콜백→프론트 핸드오프**: 백엔드 콜백이 refresh 쿠키 설정 후 프론트 redirect-uri 로 302, access token 은 URL fragment(`#accessToken=`)로 전달.
6. **태그 제약**: GIF 당 최대 20개, 태그당 최대 30자, `trim` + 소문자 정규화.
7. **제목/설명/신고 길이**: title ≤ 100, description ≤ 1000, reasonTitle ≤ 100, detail ≤ 1000.
8. **presigned TTL** 60초, **업로드 max** 50MB — env 로 조정.
9. **`blindedByAdmin` GIF 의 소유자 노출**: 마이페이지(`/users/me/gifs`)에서는 blinded 상태로 표기해 보이되, 검색/상세/raw 에서는 숨김. (소유자가 직접 삭제는 가능.)
10. **width/height 메타데이터 추출**: 선택 사항 — 미구현해도 무방(엔티티 nullable).

알려진 한계(설계상 수용):
- Stateless refresh → 탈취 토큰 즉시 폐기 불가.
- `DELETE` 처리된 GIF 의 신고 감사 이력 미보존(cascade).
- Offset 페이지네이션의 경계 drift.

---

## 14. 구현 순서 (마일스톤)

1. **기반**: 의존성 추가, `global.config`(Security/CORS/S3/DataGSM/Jackson), the-sdk 연동 확인, docker-compose(MySQL+SeaweedFS) 기동.
2. **인증**: DataGSM 로그인/콜백 → 유저 프로비저닝 → 자체 JWT(발급/필터/재발급/로그아웃), `@CurrentUser`. 비학생 거부.
3. **GIF 업로드/조회**: 엔티티 + 스토리지 어댑터 + 업로드 검증 + presigned 서빙(`/raw`).
4. **검색/목록**: keyword + 정렬 + 가시성 필터 + 페이지네이션, 마이페이지(내 GIF).
5. **태그**: Tag 엔티티 + find-or-create + 검색 통합.
6. **추천**: Like 엔티티 + 카운터 + 인기순 정렬 + 내가 추천한 GIF.
7. **공유수**: share 카운터 API.
8. **수정/삭제**: 소유자 메타데이터 수정, hard delete(+ 객체/연관 cascade).
9. **신고**: 신고 작성 + 관리자 목록/처리(NO_ISSUE/BLIND/DELETE) + blindedByAdmin 반영.
10. **마무리**: Swagger 점검, CORS/쿠키 실환경 검증.
