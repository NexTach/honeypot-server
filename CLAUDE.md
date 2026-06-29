# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

GSM 학생용 GIF 공유/검색 플랫폼 백엔드 (`team.themoment.honeypotserver`). 전체 설계 명세: @docs/implementation-spec.md

## Stack

- Kotlin 2.3.21, Java 25 toolchain, Spring Boot 4.1 (Spring **WebMVC**, 서블릿)
- Spring Data JPA + MySQL — `ddl-auto=update`, **Flyway 없음**(스키마는 엔티티에서 생성)
- Spring Security + 자체 JWT, SeaweedFS(S3 호환, AWS SDK v2), DataGSM OAuth
- 팀 공용 SDK: `the-sdk`, `datagsm-oauth-sdk-java` (jitpack 배포)

## Build & verify

- 컴파일/검증: `./gradlew build -x test`
- ⚠️ `./gradlew build`(테스트 포함)는 **실패한다** — 유일한 테스트 `HoneypotServerApplicationTests`가 Spring 컨텍스트를 띄워 MySQL/SeaweedFS/env를 요구한다. 인프라 없이 검증할 땐 항상 `-x test`. (명세상 단위테스트는 당장 없음 — §2.20.)
- 로컬 인프라: `docker-compose up` (MySQL + SeaweedFS). 모든 시크릿·엔드포인트는 env.

## Package layout

도메인은 `domain.<도메인>` 아래 4계층, 공통 인프라는 `global` 아래:

```
domain.<gif|user|tag|like|report>.{domain, application, infra, presentation}
global.{config, security, common}
```

- `domain`: 엔티티 / enum / 리포지토리 인터페이스 + 도메인 규칙
- `application`: 서비스(`@Transactional` 경계)
- `infra`: JPA 리포지토리, 외부 어댑터(S3 / DataGSM)
- `presentation`: 컨트롤러 + request/response DTO

## Conventions — the-sdk (직접 구현 금지)

- 컨트롤러는 **raw DTO를 반환**한다. `the-sdk`가 `CommonApiResponse{status, code, message, data}`로 자동 래핑하므로 응답 래퍼를 직접 만들지 말 것.
- 예외는 `team.themoment.sdk.exception.ExpectedException(message, org.springframework.http.HttpStatus)`를 throw. `GlobalExceptionHandler` / 에러코드 enum / Swagger 설정을 직접 추가하지 말 것 — 전부 the-sdk가 제공.
- 인증 사용자 주입은 `@CurrentUser principal: AuthPrincipal`(`global.security`). permitAll 경로에선 nullable `AuthPrincipal?`로 받으면 미인증 시 `null`이 주입된다(non-null이면 예외). 인가 경로는 `global.config.SecurityConfig` 한 곳에서 패턴으로 관리(`/v1/admin/**` = ROLE_ADMIN, `GET /v1/gifs/*/raw` = permitAll, 컨트롤러 빈은 자동 등록).

## Domain gotchas

- JPA 엔티티는 `allOpen` 플러그인 대상(Entity / MappedSuperclass / Embeddable). 감사 필드는 `global.common.BaseTimeEntity` 상속.
- GIF는 **hard delete**: `Like`·`Report`의 `Gif` FK에 `@OnDelete(action = CASCADE)`로 연쇄 삭제. 관리자 강제 삭제는 `GifCommandService.forceDeleteGifById`(소유권 검증 우회).
- 비정규화 카운터(`likeCount` / `shareCount`)는 `GifRepository`의 `@Modifying` 원자 UPDATE(`incrementLikeCount` 등)로만 증감 — 엔티티 setter로 직접 조작하지 말 것.
- 미디어 서빙(`GET /v1/gifs/{id}/raw`)은 **백엔드 바이트 프록시**(presigned/리다이렉트 아님). SeaweedFS는 인증 전용 버킷이고, 공개/비공개는 `GifMediaService.openAuthorizedStream`이 `GifQueryService.checkVisibility`로 매 요청 판정 — 공개 GIF는 무인증 스트리밍, 비공개·블라인드는 404. 외부 메신저(Discord) 링크 임베드를 위해 이 경로만 permitAll.
