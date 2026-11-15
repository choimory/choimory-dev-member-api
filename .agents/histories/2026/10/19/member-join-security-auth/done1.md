# 목차

- [개요](#개요)
- [최종 결과](#최종-결과)
  - [API](#api)
  - [인증 흐름](#인증-흐름)
  - [패키지 구조](#패키지-구조)
- [작업 요약](#작업-요약)
  - [회원가입 API](#회원가입-api)
  - [1단계: Security 기본 설정](#1단계-security-기본-설정)
  - [2단계: JWT](#2단계-jwt)
  - [3단계: Redis 설정과 로그인](#3단계-redis-설정과-로그인)
  - [4단계: 로그아웃과 Access Token 블랙리스트](#4단계-로그아웃과-access-token-블랙리스트)
  - [구조 정리](#구조-정리)
- [주요 결정과 이유](#주요-결정과-이유)
- [검증 결과](#검증-결과)
- [남은 사항](#남은-사항)

---

# 개요

- 작업명: 회원가입 API 및 Spring Security 설정, 로그인/로그아웃 필터 작업 (`member-join-security-auth`)
- 기간: 2026-10-19
- 회원가입 API를 구현하고, Spring Security 기본 구조와 JWT, Redis 기반 Refresh Token 저장소를 만들어 로그인, 토큰 재발급, 로그아웃을 구현했다.
- 분석과 논의는 plan1~4.md, 단계별 작업 내용은 task1~4.md에 있다. 이 문서는 전체 결과를 요약한다.

---

# 최종 결과

## API

| API | 처리 | 인증 | 응답 |
|---|---|---|---|
| `POST /v1/member/join` 회원가입 | `MemberCommandController` | 불필요 | 201, 400, 409(`DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`), 500 |
| `POST /v1/login` 로그인 | `LoginFilter` | 불필요 | 200(토큰), 400, 401, 500 |
| `POST /v1/member/reissue` 토큰 재발급 | `MemberCommandController` | 불필요 | 200(토큰), 400, 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500 |
| `POST /v1/logout` 로그아웃 | `LogoutFilter` | 불필요 (Refresh Token이 기준) | 200, 400, 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500 |

- 명세서: `docs/api/v1/choimory-dev-member-api-member-v1.json`(회원가입, 재발급), `docs/api/v1/choimory-dev-member-api-security-v1.json`(로그인, 로그아웃). 두 문서 모두 OpenAPI 3.1 규격 검증을 통과했다.

## 인증 흐름

- 로그인: 이메일과 비밀번호를 검증하고(`ACTIVATE` 상태의 회원만), Access Token(30분)과 Refresh Token(14일)을 발급한다. Refresh Token은 Redis에 `refresh:{memberId}:{jti}`로 저장해 기기별 다중 로그인을 지원한다.
- 요청 인증: `Authorization: Bearer {accessToken}`을 `JwtAuthenticationFilter`가 서명, 만료, 종류, 블랙리스트를 확인해 검증한다. 토큰의 클레임만으로 인증하고 DB를 조회하지 않는다.
- 재발급(rotation): Refresh Token을 Redis에서 삭제하는 데 성공한 요청만 통과한다. 회원의 현재 상태(`ACTIVATE`)와 권한을 다시 확인한 뒤 새 토큰 한 쌍을 발급한다.
- 로그아웃: Refresh Token을 폐기하고, 같은 회원의 유효한 Access Token은 `blacklist:{jti}`로 남은 만료 시간만큼 저장한다. Access Token이 없거나 만료되었어도 로그아웃된다.
- 오류: 만료는 `TOKEN_EXPIRED`, 그 외는 `INVALID_TOKEN`으로 401을 응답한다. 없는 이메일, 틀린 비밀번호, 로그인할 수 없는 상태의 회원은 구분하지 않고 모두 같은 401을 응답해 계정 정보를 노출하지 않는다.
- Redis 장애: 블랙리스트를 확인하지 못하면 인증하지 않는다(fail-closed).

## 패키지 구조

```
dev.choimory.member.api
├── config/                 JpaConfig, PasswordEncoderConfig, SecurityConfig
├── common/exception/       CommonException, CommonExceptionHandler
├── member/v1/command/      controller, service, handler, repository, payload/{request,response}
├── jwt/v1/command/
│   ├── handler/            JwtCommandHandler
│   ├── repository/         JwtRepository (Redis)
│   ├── util/               JwtProvider
│   └── payload/{dto,type}  JwtClaims, IssuedToken, IssuedTokens, JwtProperties, TokenType
└── security/
    ├── filter/             JwtAuthenticationFilter, LoginFilter, LogoutFilter
    ├── handler/            LoginSuccessHandler, LoginFailureHandler, UnauthorizedHandler, ForbiddenHandler, SecurityResponseWriter
    ├── payload/request/    LoginRequest, LogoutRequest
    └── user/               AuthenticatedMember, MemberUserDetailsService
```

---

# 작업 요약

## 회원가입 API

- `POST /v1/member/join`: 이메일, 비밀번호, 닉네임, 자기소개를 받아 회원을 `ACTIVATE` 상태로 저장하고 기본 권한 `MEMBER`를 부여한다.
- 입력 검증: 이메일 형식(`a@b` 거부), 비밀번호 8~64자에 영문·숫자·특수문자 포함, 닉네임 한글·영문·숫자만 3~15자, 자기소개 500자 이하
- 중복 검증: soft delete된 회원의 이메일·닉네임도 hard delete 배치 전까지는 사용할 수 없으므로, 삭제된 행까지 보는 네이티브 쿼리로 사전 검증한다. 사전 검증을 통과한 동시 요청은 저장 시점의 유니크 제약 위반(`DataIntegrityViolationException`)을 제약명으로 409로 변환한다.
- 비즈니스 예외는 별도 예외를 만들지 않고 `CommonException`으로 던진다(핸들러에서 로그·알람으로 critical 여부를 일괄 분류하기 위해).
- 권한 기본 데이터(`MEMBER`, `ADMIN`)는 `V3__init_authority_data.sql` 마이그레이션과 참고용 DML에 고정 UUID v7로 넣었다. 약관 검증·동의 이력 저장과 이메일 인증 queue 전송은 TODO로 남겼다.
- `member-domain` 테스트는 `docs/flyway`를 모두 적용하는데, 설명에 `data`가 포함된 데이터 전용 마이그레이션(V3)은 건너뛰도록 `MigrationScripts`를 고쳤다.
- 상세: task1.md

## 1단계: Security 기본 설정

- `SecurityConfig`(`config` 패키지): CSRF, 폼 로그인, HTTP Basic, 기본 로그아웃을 끄고 stateless 세션을 사용한다. `@EnableMethodSecurity`를 켰다.
- `UnauthorizedHandler`(401), `ForbiddenHandler`(403), `SecurityResponseWriter`: 필터 단계의 인증·인가 실패는 `@RestControllerAdvice`에 닿지 않으므로 공통 응답 JSON을 직접 작성한다.
- 상세: task2.md

## 2단계: JWT

- jjwt 0.12.6, HS256, 서명 키는 환경변수 `jwt-secret`(32바이트 이상), 설정은 `application-jwt.yml`
- `JwtProvider`: 토큰 발급과 검증. 서명, 발급자, 만료, 종류(access/refresh)를 검증하고 만료는 `TOKEN_EXPIRED`, 그 외는 `INVALID_TOKEN`으로 구분한다.
- `JwtAuthenticationFilter`: Access Token으로 인증 정보를 채운다. 토큰 오류는 요청 속성에 담아 두었다가 인증이 필요한 주소에서만 진입점이 401로 응답하므로 가입 같은 열린 주소는 잘못된 토큰이어도 처리된다.
- `@PreAuthorize`가 던지는 `AccessDeniedException`이 500이 되는 문제를 테스트로 찾아 `CommonExceptionHandler`에 403 처리를 추가했다.
- 상세: task2.md

## 3단계: Redis 설정과 로그인

- Redis: `application-redis.yml`(환경변수 `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`), 테스트는 Testcontainers Redis 사용
- `LoginFilter`: `AbstractAuthenticationProcessingFilter` 기반. JSON 본문을 읽어 인증하고, 본문이 JSON이 아니거나 비어 있으면 400, 인증 실패는 401을 응답한다.
- `MemberUserDetailsService`, `AuthenticatedMember`, `LoginSuccessHandler`(토큰 발급), `LoginFailureHandler`
- JSON이 아닌 요청 본문(`HttpMessageNotReadableException`)이 500이던 문제를 400으로 고쳤다. 회원가입 API에도 해당한다.
- 상세: task3.md

## 4단계: 로그아웃과 Access Token 블랙리스트

- `LogoutFilter`: Spring `logout()` 대신 직접 만든 필터. Refresh Token이 인증 기준이며 Authorization 헤더의 Access Token은 선택이다.
- `JwtRepository`의 블랙리스트(`blacklist:{jti}`)와 `JwtAuthenticationFilter`의 블랙리스트 확인
- Redis 장애 시 fail-closed. 인증이 필요한 주소만 500 `CommonResponse`로 응답하고 가입·로그인·재발급·로그아웃은 영향받지 않는다.
- 상세: task4.md

## 구조 정리

작업 중 구조가 몇 차례 바뀌었다. 최종 결과는 위 "최종 결과"가 기준이다.

- `auth` 도메인을 만들었다가 `jwt` 도메인으로 바꾸고, 다시 컨트롤러·서비스를 없애 `jwt`에는 핸들러·리포지토리·유틸·데이터 객체만 남겼다. 토큰 재발급은 `member` 컨트롤러(`/v1/member/reissue`)로 옮겼다.
- 로그인은 한때 컨트롤러로 구현했다가 원래 설계대로 필터(`LoginFilter`)로 되돌렸다. 로그인 주소는 `/v1/jwt/login`에서 `/v1/login`으로 바꿨다.
- `common/security` 패키지를 `security`로, `SecurityConfig`를 `config`로 옮겼다.
- 핸들러 이름에서 `Common` 접두어를 없애 `UnauthorizedHandler`, `ForbiddenHandler`로 정했다.
- 데이터 객체(`JwtClaims` 등)와 `JwtProperties`는 `jwt/v1/command/payload/{dto,type}`로, `JwtProvider`는 `jwt/v1/command/util`로 옮겼다. `payload`는 요청·응답뿐 아니라 데이터 객체를 모두 담는 패키지로 쓴다.
- OpenAPI 문서는 `member`(가입, 재발급)와 `security`(로그인, 로그아웃) 두 개이다.

---

# 주요 결정과 이유

| 결정 | 이유 |
|---|---|
| 키 하나(`jwt-secret`)로 Access/Refresh Token을 서명 | 같은 서버의 같은 환경변수라 키를 나눠도 유출 시 이점이 거의 없다. 종류 혼용은 `type` 클레임으로 막는다. |
| Refresh Token을 Redis에 저장하고 rotation | 로그아웃으로 폐기할 수 있고 한 번 쓴 토큰의 재사용을 막는다. |
| 요청 인증은 토큰 클레임만 사용, 블랙리스트만 Redis 조회 | 요청마다 DB를 조회하지 않는다. |
| 로그아웃은 Refresh Token 기준 | Access Token이 만료된 상태에서도 로그아웃할 수 있다. |
| 로그인·로그아웃 필터를 스프링 빈으로 등록하지 않고 직접 생성 | 빈으로 등록하면 서블릿 필터로 한 번 더 등록된다. |
| 로그인 실패는 모두 같은 401 | 계정 존재 여부와 상태가 노출되지 않게 한다. |
| 재발급을 `member` 컨트롤러로 | 회원 상태·권한 조회가 `member` 안에서 끝나고 `jwt`는 토큰 처리만 하는 하위 모듈이 된다. |
| 데이터 객체를 `member-api`의 `payload`에 두고 `member-domain`에 두지 않음 | `member-domain`의 `command/dto`는 엔티티와 1:1인 DTO 전용이고, 토큰은 API 계층의 관심사이며 쓰는 곳이 `member-api`뿐이다. |

---

# 검증 결과

- `./gradlew clean build` 성공
- `member-api` 테스트 83개, `member-domain` 테스트 107개 모두 성공 (Testcontainers PostgreSQL 16, Redis 7 사용, Docker 필요)
- `spotlessApply` 적용, spotless 검사 통과
- OpenAPI 3.1 규격 검증(`openapi-spec-validator`)으로 `member`, `security` 문서 통과

---

# 남은 사항

- 약관 검증과 동의 이력 저장, 이메일 인증 queue 전송은 회원가입의 TODO이다. 이메일 인증을 도입할 때 가입 직후 상태를 `REGISTER`로 두고 인증 후 `ACTIVATE`로 올린다.
- Redis 장애 시 fail-closed 동작의 자동 테스트와, ACL이 설정된 Redis(`redis-username`)에 대한 접속 확인은 하지 않았다.
- Apidog 가져오기는 직접 확인하지 못했다.
- 메뉴 기본 데이터는 메뉴가 구체화될 때 `V` 마이그레이션으로 추가한다.
- 다른 도메인(`authority`, `agreement`, `menu`)의 API와 query(Elasticsearch)는 클래스 뼈대만 있다.
- 히스토리 문서(plan2~4.md, task3~4.md)에는 중간에 바뀐 구조(`auth` 도메인, `/v1/jwt/*` 경로, `Common` 접두어 핸들러 등)가 남아 있다. 각 문서의 정정 섹션과 이 문서의 "최종 결과"가 최종 기준이다.
