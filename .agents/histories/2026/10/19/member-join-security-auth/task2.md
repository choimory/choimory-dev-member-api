# 목차

- [개요](#개요)
- [Security 기본 설정 (1단계)](#security-기본-설정-1단계)
  - [답변](#답변)
  - [확정한 결정](#확정한-결정)
  - [작업 내용](#작업-내용)
  - [테스트](#테스트)
  - [검증 결과](#검증-결과)
  - [참고](#참고)
  - [남은 사항](#남은-사항)

---

# 개요

- plan2.md에서 설계한 Spring Security, JWT 작업을 단계별로 구현한다.
- 작업 순서: (1) Security 기본 설정 (2) JWT (3) Redis 설정과 로그인(토큰 발급) (4) 로그아웃 (5) 토큰 재발급 API
- 이 문서는 1단계를 기록한다. 이후 단계는 진행할 때 이 문서에 이어서 추가한다.
- 참고: plan2.md, task1.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# Security 기본 설정 (1단계)

> 추천안대로 진행해줘

## 답변

plan2.md의 "남은 결정 7가지"를 추천안으로 확정하고, 1단계(Security 기본 설정)를 구현했다. 로컬에서 Apidog으로 호출했을 때 가입 API가 401로 막히던 문제(`SecurityConfig` 없음)를 이 단계에서 해결한다. JWT 이후 단계는 이 단계를 확인받은 뒤 진행한다.

## 확정한 결정

| # | 항목 | 결정 |
|---|---|---|
| 1 | Refresh Token 전달 | 응답 본문 |
| 2 | Access Token 블랙리스트 | 로그아웃 시 사용 |
| 3 | 로그인 허용 범위 | 다중 로그인 허용 (`refresh:{memberId}:{jti}`) |
| 4 | JWT 라이브러리 | jjwt 0.12.x |
| 5 | 로그아웃 | Spring `logout()` 설정 + 핸들러 |
| 6 | `auth` 도메인, 로그인 가능 상태 | `auth` 도메인 추가, `ACTIVATE`만 로그인, 실패 응답은 모두 같은 401 |
| 7 | 만료 시간 | Access 30분, Refresh 14일 |

## 작업 내용

### SecurityConfig

- 파일: `common/security/config/SecurityConfig`
- CSRF, 폼 로그인, HTTP Basic, 기본 로그아웃을 끄고 세션은 stateless로 둔다. 로그아웃은 4단계에서 `logout()` 설정으로 다시 켠다.
- `POST /v1/member/join`만 `permitAll`이고 나머지는 `authenticated()`이다. 로그인과 재발급 주소는 해당 API가 생기는 단계에서 허용에 추가한다.
- `@EnableMethodSecurity`를 켜 두었다. 메뉴·권한 기반 동적 인가는 이번 범위가 아니다.
- CORS는 클라이언트가 정해지면 설정하도록 TODO 주석만 남겼다. 처음에 `cors(Customizer.withDefaults())`를 넣었다가 CORS 설정이 없어 불필요한 동작을 만들 수 있어 줄을 빼고 주석만 남겼다.
- 인증 실패와 권한 없음은 아래 핸들러가 공통 응답 JSON으로 내려준다.

### 응답 핸들러

- 파일: `common/security/handler`
- `SecurityResponseWriter`: HTTP 상태의 기본값(`code`, `name`, `message`)으로 `CommonResponse`(data 없음)를 UTF-8 JSON으로 쓴다. 필터 단계에서 발생한 예외는 `@RestControllerAdvice`에 닿지 않아서 직접 작성한다.
- `CommonAuthenticationEntryPoint`: 인증되지 않은 요청에 401을 내려준다. 인증 실패 사유(계정 존재 여부 등)는 노출하지 않는다.
- `CommonAccessDeniedHandler`: 인증은 되었으나 권한이 없는 요청에 403을 내려준다.
- 응답 형태: `{"code":401,"name":"UNAUTHORIZED","message":"Unauthorized","data":null}`

## 테스트

- `SecurityConfigTest`(신규, Testcontainers): 필터를 켠 상태로 확인한다.
  - 인증 없이 보호된 주소를 호출하면 JSON 형태의 401이다.
  - CSRF를 껐으므로 인증 없는 POST, DELETE도 403이 아니라 401이다.
  - 가입 주소는 인증 없이 열려 있어 빈 요청이어도 401이 아니라 400이다.
  - 가입 주소를 GET으로 호출하면 열려 있지 않아 401이다.
- `SecurityHandlersTest`(신규, Docker 불필요): 진입점의 401, 접근 거부 핸들러의 403 본문이 공통 응답 JSON인지, 실패 사유(예외 메시지)가 노출되지 않는지, UTF-8인지 확인한다.
- `MemberJoinTest`: `@AutoConfigureMockMvc(addFilters = false)`에서 `@AutoConfigureMockMvc`로 바꿔 Security 필터를 켠 상태로 검증한다. 기존 가입 테스트가 필터를 통과해 그대로 성공했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 39개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 앱 시작 시 `Using generated security password: ...` 로그가 계속 보일 수 있다. Spring Boot가 `UserDetailsService`가 없어서 기본 계정을 만드는 것으로, 3단계에서 `MemberUserDetailsService`를 만들면 사라진다. 폼 로그인과 HTTP Basic을 껐으므로 이 기본 계정은 실제로 쓸 수 없다.
- OpenAPI 명세서(`choimory-dev-member-api-member-v1.json`)의 가입 API는 `permitAll`이라 바뀌는 내용이 없다. 인증이 필요한 API가 생길 때 401·403 응답을 추가한다.

## 남은 사항

- 2단계(JWT): jjwt 의존성과 `jwt-secret` 설정, `JwtProperties`, `JwtProvider`, `JwtAuthenticationFilter`, `AuthenticatedMember`
- 3단계: Redis 설정(`application-redis.yml`, 테스트용 Redis Testcontainers), 로그인 필터, `MemberUserDetailsService`, `auth` 도메인(`AuthCommandHandler`, `AuthTokenRepository`)
- 4단계: `logout()` 설정, 핸들러, Access Token 블랙리스트
- 5단계: 토큰 재발급 API (`POST /v1/auth/reissue`)
- `auth` 도메인용 OpenAPI 명세서(`choimory-dev-member-api-auth-v1.json`)는 해당 API가 생길 때 작성한다.
- plan2.md에는 7가지 결정 사항이 아직 기록되어 있지 않다. README에는 반영하지 않았다.
