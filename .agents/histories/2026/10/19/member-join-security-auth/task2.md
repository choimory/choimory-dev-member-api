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
- [JWT (2단계)](#jwt-2단계)
  - [답변](#답변-1)
  - [작업 내용](#작업-내용-1)
  - [테스트](#테스트-1)
  - [테스트로 찾은 문제](#테스트로-찾은-문제)
  - [검증 결과](#검증-결과-1)
  - [참고](#참고-1)
  - [남은 사항](#남은-사항-1)

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

- 파일: `config/SecurityConfig` (처음에는 `common/security/config`에 만들었다가 `JpaConfig`, `PasswordEncoderConfig`와 같은 `config` 패키지로 옮겼다)
- CSRF, 폼 로그인, HTTP Basic, 기본 로그아웃을 끄고 세션은 stateless로 둔다. 로그아웃은 4단계에서 `logout()` 설정으로 다시 켠다.
- `POST /v1/member/join`만 `permitAll`이고 나머지는 `authenticated()`이다. 로그인과 재발급 주소는 해당 API가 생기는 단계에서 허용에 추가한다.
- `@EnableMethodSecurity`를 켜 두었다. 메뉴·권한 기반 동적 인가는 이번 범위가 아니다.
- CORS는 클라이언트가 정해지면 설정하도록 TODO 주석만 남겼다. 처음에 `cors(Customizer.withDefaults())`를 넣었다가 CORS 설정이 없어 불필요한 동작을 만들 수 있어 줄을 빼고 주석만 남겼다.
- 인증 실패와 권한 없음은 아래 핸들러가 공통 응답 JSON으로 내려준다.

### 응답 핸들러

- 파일: `security/handler` (처음에는 `common/security/handler`에 만들었다가 `common`을 거치지 않는 `security` 패키지로 옮겼다. 테스트도 `test/.../api/security`로 옮겼다)
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

- 2단계(JWT): jjwt 의존성과 `jwt-secret` 설정, `JwtProperties`와 `JwtProvider`(`api/jwt` 패키지), `JwtAuthenticationFilter`, `AuthenticatedMember`
- 3단계: Redis 설정(`application-redis.yml`, 테스트용 Redis Testcontainers), 로그인 필터, `MemberUserDetailsService`, `auth` 도메인(`AuthCommandHandler`, `AuthTokenRepository`)
- 4단계: `logout()` 설정, 핸들러, Access Token 블랙리스트
- 5단계: 토큰 재발급 API (`POST /v1/auth/reissue`)
- `auth` 도메인용 OpenAPI 명세서(`choimory-dev-member-api-auth-v1.json`)는 해당 API가 생길 때 작성한다.
- plan2.md에는 7가지 결정 사항이 아직 기록되어 있지 않다. README에는 반영하지 않았다.

---

# JWT (2단계)

> 네 2단계 시작해줘

## 답변

JWT 발급·검증과 인증 필터를 구현했다. 1단계 이후 `common/security`를 `security`로 옮기고 `jwt`는 `api` 바로 아래 패키지로 두기로 한 결정(plan2.md 참고)에 따라 `dev.choimory.member.api.jwt`, `dev.choimory.member.api.security`에 만들었다. Redis 블랙리스트 확인은 4단계로 미뤘다.

## 작업 내용

### 의존성과 설정

- `member-api/build.gradle`: jjwt 0.12.6(`jjwt-api`, 런타임 `jjwt-impl`, `jjwt-jackson`)을 추가했다.
- `application-jwt.yml`(신규): `datasource`처럼 프로필로 분리했고 `application.yml`의 `spring.profiles.include`를 `datasource, jwt`로 바꿔 항상 포함한다.
  - `jwt.secret: ${jwt-secret}` (환경변수 `jwt-secret`, HS256, 32바이트 이상)
  - `jwt.issuer: choimory-dev-member-api`
  - `jwt.access-expiration: 30m`, `jwt.refresh-expiration: 14d`
- 테스트의 서명 키는 `build.gradle`의 `test` 태스크에서 시스템 프로퍼티 `jwt-secret`으로 전달한다. 테스트 전용 값이다. `member-domain`이 `docs.flyway.dir`을 전달하는 방식과 같다.

### jwt 패키지 (`dev.choimory.member.api.jwt`)

- `JwtProperties`: `secret`, `issuer`, `accessExpiration`, `refreshExpiration`을 담는 record. `@Validated`로 필수 값을 검증한다.
- `JwtProvider`: HS256으로 토큰을 발급하고 검증한다.
  - `createAccessToken(memberId, authorities)`, `createRefreshToken(memberId)`는 `IssuedToken`(토큰, jti, 만료 일시)을 반환한다. Refresh Token을 Redis에 저장할 때 jti와 만료가 필요해서 토큰 문자열과 함께 반환한다.
  - 클레임: `sub`=회원 ID, `jti`(UUID), `iss`, `iat`, `exp`, `type`("access"/"refresh"), `authorities`(Access Token에만)
  - `parse(token, 기대 종류)`: 서명, 발급자, 만료, 토큰 종류를 검증한다. 만료는 `TOKEN_EXPIRED`, 그 외 잘못된 토큰은 `INVALID_TOKEN`으로 구분하고 둘 다 `CommonException`(401)이다. Refresh Token을 Access 자리에 쓰는 것도 `INVALID_TOKEN`으로 막는다.
  - 서명 키가 32바이트보다 짧으면 시작할 때 `WeakKeyException`으로 실패한다.
- `TokenType`(ACCESS, REFRESH), `IssuedToken`, `JwtClaims`

### security 패키지 (`dev.choimory.member.api.security`)

- `filter/JwtAuthenticationFilter`: `Authorization: Bearer`의 Access Token을 검증해 인증 정보를 채운다. 토큰의 클레임만으로 인증하므로 요청마다 DB를 조회하지 않는다.
  - 토큰 오류가 있어도 여기서 응답하지 않고 요청 속성(`ERROR_ATTRIBUTE`)에 담아 다음으로 넘긴다. 인증이 필요한 주소라면 진입점이 오류 종류로 401을 응답하고, 인증이 필요 없는 주소(회원가입)는 토큰이 잘못되어도 그대로 처리된다.
  - 스프링 빈으로 등록하면 서블릿 필터로 한 번 더 등록되므로 `SecurityConfig`에서 직접 생성한다.
  - 4단계에서 로그아웃한 토큰(블랙리스트)인지 Redis에서 확인하는 부분을 `// TODO`로 남겼다.
- `user/AuthenticatedMember`: Access Token 클레임으로 만든 principal. 회원 ID와 권한(`ROLE_` 접두어)을 가진다. 비밀번호는 없다.
- `handler/CommonAuthenticationEntryPoint`: 요청 속성에 JWT 오류가 있으면 그 종류(`TOKEN_EXPIRED`, `INVALID_TOKEN`)로, 없으면 일반 `UNAUTHORIZED`로 응답한다.
- `handler/SecurityResponseWriter`: `CommonException`으로 응답을 쓰는 오버로드를 추가했다.
- `config/SecurityConfig`: `@EnableConfigurationProperties(JwtProperties.class)`를 추가하고 `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 연결했다.

## 테스트

- `JwtProviderTest`(신규, Docker 불필요): Access Token 발급과 검증 왕복, Refresh Token의 권한 없음과 14일 만료, jti 유일성, 만료 토큰(`TOKEN_EXPIRED`), 다른 키·변조·형식 오류·빈 문자열(`INVALID_TOKEN`), 다른 발급자, 토큰 종류 불일치, 약한 서명 키
- `JwtAuthenticationFilterTest`(신규, Testcontainers): 테스트 안의 컨트롤러로 확인한다.
  - 유효한 토큰의 인증(회원 ID와 `ROLE_MEMBER`), 토큰 없음·Bearer가 아닌 방식의 401(`UNAUTHORIZED`), `TOKEN_EXPIRED`, `INVALID_TOKEN`(잘못된 문자열과 Access 자리의 Refresh Token)
  - 역할 인가: MEMBER 토큰으로 ADMIN 전용 주소는 403, ADMIN 토큰은 통과
  - 가입 주소는 잘못된 토큰이어도 401이 아니라 요청 값 검증 실패 400
- `SecurityHandlersTest`: 진입점이 요청 속성의 토큰 오류로 응답하는지 추가
- `CommonExceptionHandlerTest`: 접근 거부 예외가 403으로 변환되는지 추가

## 테스트로 찾은 문제

- `@PreAuthorize`가 던지는 `AccessDeniedException`이 컨트롤러 안에서 발생하면 Security 필터의 접근 거부 핸들러가 아니라 `CommonExceptionHandler`로 온다. 기존에는 `AuthenticationException`(401)만 처리하고 `AccessDeniedException` 처리가 없어서 일반 `Exception` 핸들러에 걸려 403이 아니라 500이 되었다.
- 해결: `CommonExceptionHandler`에 `AccessDeniedException` → 403 처리를 추가했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 56개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 로컬에서 실행하려면 환경변수 `jwt-secret`(32바이트 이상)이 있어야 한다. 없으면 앱이 시작되지 않는다. IntelliJ 실행 설정에 추가해야 한다.
- 서명 키는 하나(`jwt-secret`)로 Access/Refresh Token을 함께 서명한다. 토큰 종류는 `type` 클레임으로 구분한다. 키를 종류별로 둘로 나눌지는 plan2.md에서 논의했고 아직 정해지지 않았다. 추천은 하나이다.
- 토큰의 클레임은 암호화되지 않으므로 민감한 값을 담지 않는다. 현재는 회원 ID, 권한 코드, 토큰 종류, jti, 발급자, 발급·만료 시각만 담는다.
- README에는 반영하지 않았다.

## 남은 사항

- 키를 하나로 유지할지 둘로 나눌지 결정이 필요하다.
- 3단계: Redis 설정(`application-redis.yml`, 테스트용 Redis Testcontainers), 로그인 필터, `MemberUserDetailsService`, `auth` 도메인(`AuthCommandHandler`, `AuthTokenRepository`)
- 4단계: `logout()` 설정, 핸들러, Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO)
- 5단계: 토큰 재발급 API (`POST /v1/auth/reissue`)
- `auth` 도메인용 OpenAPI 명세서(`choimory-dev-member-api-auth-v1.json`)는 해당 API가 생길 때 작성한다.
- plan2.md에는 7가지 결정 사항이 아직 기록되어 있지 않다. README에는 반영하지 않았다.
