# 목차

- [개요](#개요)
- [로그아웃과 Access Token 블랙리스트 (4단계)](#로그아웃과-access-token-블랙리스트-4단계)
  - [답변](#답변)
  - [확정한 결정](#확정한-결정)
  - [작업 내용](#작업-내용)
  - [계획과 달라진 점](#계획과-달라진-점)
  - [테스트](#테스트)
  - [OpenAPI](#openapi)
  - [검증 결과](#검증-결과)
  - [참고](#참고)
  - [남은 사항](#남은-사항)
- [API 문서 점검과 문서 이름 정리](#api-문서-점검과-문서-이름-정리)
  - [답변](#답변-1)
  - [점검 결과](#점검-결과)
  - [반영한 내용](#반영한-내용)
  - [문서 파일 이름 변경](#문서-파일-이름-변경)
  - [검증](#검증)
- [데이터 객체 위치와 핸들러 이름 변경](#데이터-객체-위치와-핸들러-이름-변경)
  - [답변](#답변-2)
  - [결정](#결정)
  - [패키지 이동](#패키지-이동)
  - [핸들러 이름 변경](#핸들러-이름-변경)
  - [검토했지만 채택하지 않은 안](#검토했지만-채택하지-않은-안)
  - [검증 결과](#검증-결과-1)
  - [참고](#참고-1)
  - [남은 사항](#남은-사항-1)
- [JwtProvider와 JwtProperties 위치 이동](#jwtprovider와-jwtproperties-위치-이동)
  - [답변](#답변-3)
  - [검토한 내용](#검토한-내용)
  - [결정](#결정-1)
  - [이동 내용](#이동-내용)
  - [검증 결과](#검증-결과-2)
  - [참고](#참고-2)

---

# 개요

- plan4.md에서 계획한 4단계(로그아웃과 Access Token 블랙리스트)를 구현한다. `POST /v1/logout`으로 로그아웃하면 Refresh Token을 폐기하고, 함께 온 유효한 Access Token은 만료 전에도 쓰지 못하게 블랙리스트에 올린다.
- 1단계(Security 기본 설정), 2단계(JWT)는 task2.md, 3단계(Redis 설정과 로그인)와 이후 구조 변경은 task3.md에 기록되어 있다. 현재 구조는 로그인 `POST /v1/login`이 `LoginFilter`, 재발급 `POST /v1/member/reissue`가 `MemberCommandController`, 토큰 처리는 `jwt` 도메인의 `JwtCommandHandler`와 `JwtRepository`가 담당한다.
- 참고: plan2.md, plan4.md, task3.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# 로그아웃과 Access Token 블랙리스트 (4단계)

> 추천안대로 4단계 시작해줘

## 답변

plan4.md의 "정해야 할 것" 5가지를 추천안으로 확정하고, 4단계를 구현했다. 이로써 plan2.md의 설계(Security, JWT, 로그인, 로그아웃, 재발급)가 모두 구현되었다.

## 확정한 결정

| # | 항목 | 결정 |
|---|---|---|
| 1 | 필터 방식 | Spring `logout()` 대신 직접 만든 `LogoutFilter` |
| 2 | 인증 기준 | Refresh Token 필수, Authorization 헤더의 Access Token은 선택 |
| 3 | 잘못된 Refresh Token | 만료는 `TOKEN_EXPIRED`, 그 외는 `INVALID_TOKEN`으로 401. 이미 폐기된 정상 토큰은 멱등하게 200 |
| 4 | Redis 장애 | 블랙리스트를 확인하지 못하면 인증하지 않는다 (fail-closed) |
| 5 | 블랙리스트된 토큰의 오류 코드 | `INVALID_TOKEN` (새 코드를 만들지 않음) |

필터 이름은 plan4.md에서 `JwtLogoutFilter`로 계획했다가 사용자 요청으로 `LogoutFilter`로 정했다. Spring Security의 `org.springframework.security.web.authentication.logout.LogoutFilter`와 이름이 같지만, `logout()` 설정을 쓰지 않아 체인에 Spring의 필터가 없고 우리 필터는 스프링 빈이 아니라 `SecurityConfig`에서 직접 생성하므로 충돌하지 않는다.

## 작업 내용

### LogoutFilter (`security/filter`)

`OncePerRequestFilter`를 상속하고 `POST /v1/logout` 요청만 처리한다. 로그인 필터 뒤에 둔다.

- 요청 본문 `{refreshToken}`을 읽는다. JSON이 아니거나 `refreshToken`이 비어 있으면 인증을 시도하지 않고 data 없는 400으로 응답한다.
- JWT 필터가 요청 속성에 담아 둔 검증된 Access Token 클레임(있다면)과 함께 `JwtCommandHandler.logout`을 호출한다.
- 성공하면 data 없는 200(`CommonResponse`)을 응답한다.
- `CommonException`(만료, 잘못된 토큰)은 해당 상태와 코드로 응답하고, Redis 오류(`DataAccessException`)는 로그를 남기고 500으로 응답한다. 토큰을 폐기하지 못했는데 성공으로 응답하지 않기 위해서이다.
- 클래스 설명(Javadoc)에 Spring의 `LogoutFilter`와 다른 클래스이고 `logout()` 설정은 쓰지 않는다고 적었다.
- 인증 기준이 Refresh Token이라 Authorization 헤더가 없거나 Access Token이 이미 만료되었어도 로그아웃할 수 있다.

### JwtCommandHandler

- `logout(refreshToken, accessClaims)`: Refresh Token을 검증하고(서명, 발급자, 만료, 종류) Redis에서 삭제한다. 이미 없어도 성공으로 처리한다(멱등). `accessClaims`가 있고 Refresh Token과 같은 회원의 것이며 남은 만료 시간이 있으면 그 `jti`를 블랙리스트에 저장한다. 다른 회원의 Access Token은 블랙리스트에 올리지 않는다.
- `isBlacklisted(jti)`: 로그아웃한 Access Token인지 확인한다.

### JwtRepository

- `saveBlacklist(jti, ttl)`, `existsBlacklist(jti)`를 추가했다. 키는 `blacklist:{jti}`, 값은 표식 `"1"`, TTL은 Access Token의 남은 만료 시간이다. TTL이 지나면 Redis에서 자동으로 사라진다.

### JwtAuthenticationFilter

- 토큰 검증 뒤 `JwtCommandHandler.isBlacklisted`로 블랙리스트를 확인한다. 있으면 `INVALID_TOKEN`으로 처리한다. 3단계에 남겨 둔 `// TODO 4단계`를 해소했다.
- 검증을 통과한 Access Token의 클레임을 요청 속성(`ACCESS_CLAIMS_ATTRIBUTE`)에 담는다. 로그아웃 필터가 블랙리스트에 올릴 때 사용한다.
- Redis 장애(`DataAccessException`)로 블랙리스트를 확인하지 못하면 인증하지 않는다(fail-closed). 로그를 남기고 500 `CommonException`을 요청 속성에 담아, 인증이 필요한 주소는 진입점이 500 `CommonResponse`로 응답하고 인증이 필요 없는 주소(가입, 로그인, 재발급, 로그아웃)는 그대로 처리되게 했다.
- 생성자에 `JwtCommandHandler`를 추가했다.

### SecurityConfig

- `JwtCommandHandler`를 주입받아 `JwtAuthenticationFilter`에 넘기고, `LogoutFilter`를 `LoginFilter` 뒤에 직접 생성해 연결했다.
- `permitAll` 목록에 `LogoutFilter.LOGOUT_PATH`(`/v1/logout`)를 추가했다.
- 클래스 설명을 "JWT 인증 필터와 로그인, 로그아웃 필터를 연결한다"로 고쳤다.

### 그 밖의 추가

- `security/payload/request/LogoutRequest`: 로그아웃 필터가 읽는 요청 객체

## 계획과 달라진 점

- plan4.md에서는 `AuthenticatedMember`에 토큰의 `jti`와 만료 일시를 추가한다고 했다. 대신 JWT 필터가 검증된 클레임을 요청 속성에 담도록 했다. 객체를 바꾸지 않아도 되고 더 단순하다.
- plan4.md는 Redis 장애 시 "인증이 필요한 요청이 모두 500이 되는" 방식으로 적었다. 구현하면서 인증이 필요한 주소만 500 `CommonResponse`로 응답하고 인증이 필요 없는 주소는 영향받지 않게 구체화했다.
- Spring Boot의 기본 오류 응답이 아니라 `CommonResponse` 형태로 500을 내려주기 위해 JWT 필터가 직접 예외를 잡아 요청 속성에 담는 방식을 썼다.

## 테스트

- `JwtRepositoryTest`: 블랙리스트 저장·조회·TTL(30분) 테스트를 추가했다.
- `LogoutTest`(신규, Postgres + Redis) 7개
  - 로그아웃하면 Refresh Token이 삭제되고, Access Token이 `blacklist:{jti}`로 저장되며 TTL이 30분 안팎이고, 로그아웃한 Access Token으로 보호된 주소를 호출하면 `INVALID_TOKEN` 401, 그 Refresh Token으로 재발급해도 `INVALID_TOKEN`
  - 한 기기에서 로그아웃해도 다른 기기의 Refresh Token과 Access Token은 그대로 유효
  - 헤더 없음·잘못된 Access Token·이미 만료된 Access Token이어도 로그아웃되고, 블랙리스트는 생기지 않음
  - 다른 회원의 Access Token을 함께 보내면 Refresh Token만 폐기되고 그 토큰은 블랙리스트에 오르지 않으며 계속 유효
  - 같은 Refresh Token으로 두 번 로그아웃해도 모두 200(멱등)
  - 만료된 Refresh Token은 `TOKEN_EXPIRED`, 잘못된 문자열과 Access Token을 Refresh Token 자리에 보내면 `INVALID_TOKEN`이고 아무것도 폐기되지 않음
  - JSON이 아닌 본문·빈 객체·공백·빈 본문은 data 없는 400, GET은 401
- Redis 장애 시 fail-closed 동작은 Redis를 중단시키는 자동 테스트로 확인하지 못했다.

## OpenAPI

- `choimory-dev-member-api-jwt-v1.json`에 `POST /v1/logout`을 추가했다.
  - 요청 예시 2개: Access Token과 함께 보내는 경우, Refresh Token만 보내는 경우
  - 응답 예시: 200(data 없음), 400(JSON이 아닌 본문, Refresh Token 누락), 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500(Redis 장애)
  - 설명에 인증 기준이 Refresh Token인 점, Access Token이 선택인 점, 같은 회원의 Access Token만 블랙리스트에 오르는 점, 멱등인 점을 적었다.
- 스키마 `LogoutRequest`, `CommonResponseEmpty`를 추가했고 문서 상단 설명과 태그를 로그인과 로그아웃을 다루도록 고쳤다. 파일의 경로는 `/v1/login`, `/v1/logout`이다.
- JSON 파싱과 `$ref` 해석을 파이썬으로 확인했고 끊어진 참조와 쓰지 않는 스키마는 없다. 전용 검증기와 Apidog 가져오기는 확인하지 못했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 83개(`LogoutTest` 7개, `JwtRepositoryTest` 1개 추가), `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 로그아웃한 Access Token은 블랙리스트 TTL(남은 만료 시간)이 지나면 어차피 만료되어 Redis에서 사라진다. 인증이 필요한 모든 요청에서 Redis 조회가 1회 늘어난다.
- 로컬 실행 환경변수는 그대로이다: `jwt-secret`(32바이트 이상), `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`

## 남은 사항

- plan2.md에 설계한 Security, JWT, 로그인, 로그아웃, 재발급이 모두 구현되었다.
- 마무리 작업은 말씀하실 때 진행한다: README 반영(`jwt` 도메인 구성, 로그인·로그아웃이 필터라서 컨트롤러 경로 규칙의 예외라는 점, 토큰 관련 URL, `docs/api` 구성과 API 명세서 규칙, 환경변수), `done` 문서 작성, `calendar.md`를 DONE으로 변경
- 약관 검증·동의 이력 저장과 이메일 인증 queue 전송은 회원가입의 TODO로 남아 있다.
- plan2.md 구조도, plan3.md, 앞쪽 task3.md 섹션의 옛 구조(`auth` 도메인, `/v1/jwt/*` 경로 등)는 히스토리라서 수정하지 않았다.

---

# API 문서 점검과 문서 이름 정리

> api 문서 확인
>
> 1. jwt controller 없어졌는데 api 명세서에는 Jwt 도메인이 있음 이거 바꿔야하는거 아님?
> 2. 베어러 선택헤더로 추가
> 3. security/handler에 common으로 시작하는애들 common 빼도 되지 않아? 처음에 common에 만들어서 붙어있는거 같은데
> 4. jwt의 jwtclaims, issuedtoken 이런애들 다 데이터 객체같은데 jwt/v1하위에 적절한 위치에 저장하는거불가능한가?
>
> 1. 토큰 재발급은 member에 있으니 member, 로그인/로그아웃은 일단 security ㅇㅋ

## 답변

4단계 구현 후 API 문서를 점검했고, 지적받은 문서의 `jwt` 도메인 이름과 로그아웃 헤더를 반영했다. 3번과 4번은 다음 섹션에서 결정하고 반영했다.

## 점검 결과

| 항목 | 결과 |
|---|---|
| OpenAPI 3.1 규격 검증 (`openapi-spec-validator`를 임시로 설치해 실행한 뒤 삭제) | `member`, `jwt` 문서 모두 통과 |
| JSON 파싱, `$ref` 해석 | 끊어진 참조와 쓰지 않는 스키마 없음 |
| 예시와 스키마 | 모든 요청·응답 예시가 스키마의 필수 필드와 필드 이름에 맞음 |
| 예시 값과 코드 | 오류 코드명과 메시지가 코드와 일치 |
| 옛 경로·이름 | `/v1/jwt/...`, `/v1/auth/...`, `JwtTokenResponse`, `ReissueRequest`는 남아 있지 않음 |

보완할 점은 두 가지였다. 로그아웃의 `Authorization` 헤더가 설명 글에만 있고 헤더 파라미터로는 정의되지 않아 Apidog에서 입력란이 보이지 않는 것, 그리고 헤더만 다르고 본문이 똑같은 요청 예시 두 개였다. 또 로그인과 로그아웃은 `security` 패키지의 필터가 처리하는데 문서 이름이 `jwt`여서 실제 코드 위치와 어긋났다.

## 반영한 내용

- 로그아웃에 헤더 파라미터 `Authorization`(`in: header`, `required: false`, 형식 `Bearer {토큰}`)을 추가했다. 헤더 설명과 예시("Access Token과 함께" / "헤더 없음")는 파라미터에 넣었다. 본문 예시는 하나("정상 요청")로 합쳤다.
- 로그아웃 설명에 `Authorization` 헤더가 선택 파라미터임을 명시했다.

## 문서 파일 이름 변경

- 토큰 재발급은 이미 `member` 문서(`/v1/member/reissue`)에 있고, 로그인과 로그아웃은 `security`로 정했다.
- `choimory-dev-member-api-jwt-v1.json`을 `choimory-dev-member-api-security-v1.json`으로 바꿨다. 제목, 태그, 상단 설명을 `security` 기준으로 고쳤고 설명에는 "Spring Security 필터가 처리하는 로그인과 로그아웃을 다루고, 재발급은 컨트롤러라 `member` 문서에 있다"고 적었다.
- `docs/api/v1`에는 `member`(회원가입, 재발급)와 `security`(로그인, 로그아웃) 두 문서가 있고, `jwt` 문자열은 문서에 남아 있지 않다.

## 검증

- 두 문서 모두 OpenAPI 3.1 규격 검증을 통과했다. 파일 이름 변경 후에는 JSON 파싱과 경로(`/v1/login`, `/v1/logout`) 확인을 했다.
- Apidog 가져오기는 직접 확인하지 못했다. 불러온 뒤 로그아웃의 헤더 입력란이 보이는지는 사용자가 확인해야 한다.

---

# 데이터 객체 위치와 핸들러 이름 변경

> member-api말고 Member-domain 모듈은 어때?
>
> jwt.v1.command.payload.dto, type으로 해줘. payload는 request, response 뿐 아니라 데이터객체를 다 저장할 패키지였는데 내가 네이밍이 마땅치 않아서 payload라고 한것일 뿐임
> 2. 그 이름으로 해줘

## 답변

`JwtClaims`, `IssuedToken`, `IssuedTokens`, `TokenType`을 `jwt/v1/command/payload` 아래로 옮기고, 핸들러 이름에서 `Common` 접두어를 없앴다.

## 결정

- 데이터 객체는 `member-api`의 `jwt/v1/command/payload/dto`, `payload/type`에 둔다. `payload`는 요청·응답 객체뿐 아니라 데이터 객체를 모두 담는 패키지라는 설명을 반영했다.
- 핸들러 이름은 제안한 `UnauthorizedHandler`(401), `ForbiddenHandler`(403)로 한다.

## 패키지 이동

| 클래스 | 이동 |
|---|---|
| `JwtClaims`, `IssuedToken`, `IssuedTokens` | `jwt/` → `jwt/v1/command/payload/dto` |
| `TokenType`(enum) | `jwt/` → `jwt/v1/command/payload/type` |
| `JwtProvider`, `JwtProperties` | `jwt/` 루트 유지(토큰 생성·검증 컴포넌트와 설정 객체) |

- 패키지 선언과 import를 모두 고쳤다. 같은 패키지라서 import가 필요 없던 `JwtProvider`, `JwtClaims`(`TokenType` 사용)에는 import를 추가했다. 처음에 `JwtClaims`의 `TokenType` import를 놓쳐 컴파일이 실패했고, 추가해서 해결했다.
- 영향받은 파일은 main 8개, test 4개 안팎(`JwtProvider`, `JwtCommandHandler`, `MemberTokenResponse`, `MemberCommandService`, `JwtAuthenticationFilter`, `LogoutFilter`, `AuthenticatedMember`, `JwtProviderTest`, `MemberReissueTest`, `LoginTest`, `LogoutTest` 등)이다. 동작은 바뀌지 않는다.

## 핸들러 이름 변경

| 이전 | 이후 |
|---|---|
| `CommonAuthenticationEntryPoint` | `UnauthorizedHandler` |
| `CommonAccessDeniedHandler` | `ForbiddenHandler` |

- `Common` 접두어는 처음에 `common/security` 아래에 만들어서 붙은 이름이었다. 접두어만 빼면 구현하는 Spring 인터페이스(`AuthenticationEntryPoint`, `AccessDeniedHandler`)와 이름이 같아져서, 인터페이스와 겹치지 않는 역할 이름으로 바꿨다.
- `SecurityConfig`의 필드도 `unauthorizedHandler`, `forbiddenHandler`로 바꿨고, `SecurityHandlersTest`의 참조도 맞췄다.
- `LoginSuccessHandler`, `LoginFailureHandler`, `SecurityResponseWriter`는 그대로이다.

## 검토했지만 채택하지 않은 안

- `member-domain` 모듈에 두는 안: `command/dto`는 엔티티와 1:1인 DTO 전용이고 `DtoRulesTest`가 검사하며, 도메인 모듈의 `type`은 DB에 저장되는 enum이라 `TokenType`과 성격이 다르다. 또 쓰는 곳이 `member-api`뿐이고 영속 모델 중심의 도메인 모듈에 토큰 개념이 들어간다. 다른 모듈이 토큰을 읽어야 할 때 필요한 객체만 공유 모듈로 올리기로 했다.
- `Rest` 접두어(`RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`)와 인터페이스와 같은 이름 그대로 쓰는 안: 전자는 접두어를 모두 없애려는 요청에 맞지 않고, 후자는 구현하는 쪽에서 인터페이스를 패키지까지 풀어 써야 해서 읽기 어렵다.
- `jwt/v1/command/dto`, `type`을 `payload`와 같은 단계에 두는 안: 제가 `payload`를 요청·응답 전용으로 해석해서 추천한 안이었는데, 사용자가 `payload`는 데이터 객체 전체를 담는 패키지라고 설명해 주어 `payload` 아래로 정정했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 83개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 현재 구조

```
jwt/
├── JwtProperties, JwtProvider
└── v1/command/
    ├── handler/JwtCommandHandler
    ├── repository/JwtRepository
    └── payload/{dto, type}
security/
├── filter/{JwtAuthenticationFilter, LoginFilter, LogoutFilter}
├── handler/{LoginSuccessHandler, LoginFailureHandler, SecurityResponseWriter, UnauthorizedHandler, ForbiddenHandler}
├── payload/request/{LoginRequest, LogoutRequest}
└── user/{AuthenticatedMember, MemberUserDetailsService}
```

- plan4.md의 "4단계 구현 후 API 문서 점검과 정리 제안"과 "문서 이름 결정과 핸들러 이름, 데이터 객체 위치" 섹션에는 `dto`, `type`을 `payload`와 같은 단계에 두는 안과 `Rest`, `Unauthorized...` 이름 논의가 남아 있고 실제 결정과 다르다. 히스토리라서 수정하지 않았다. 이 섹션이 최종 결정이다.

## 남은 사항

- 마무리 작업은 말씀하실 때 진행한다: README 반영(`jwt` 도메인 구성과 `payload` 사용 방식, 로그인·로그아웃이 필터라서 컨트롤러 경로 규칙의 예외라는 점, 토큰 관련 URL, `docs/api` 구성(`member`, `security`)과 API 명세서 규칙, 환경변수), `done` 문서 작성, `calendar.md`를 DONE으로 변경
- 약관 검증·동의 이력 저장과 이메일 인증 queue 전송은 회원가입의 TODO로 남아 있다.
- Redis 장애 시 fail-closed 동작의 자동 테스트는 아직 없다.

---

# JwtProvider와 JwtProperties 위치 이동

> jwtprovider는 jwt.v1.command.util에 놓으면 별론가
>
> properties는 yml값을 담은 데이터객체아냐? dto가 맞지 않나?
>
> 네 그 구조로 옮겨줘

## 답변

`jwt/` 루트에 남아 있던 `JwtProvider`와 `JwtProperties`를 `jwt/v1/command` 아래로 옮겼다. 이로써 `jwt/` 루트에는 파일이 없고 모든 클래스가 `v1/command` 아래에 있다.

## 검토한 내용

- `JwtProvider`를 `util`에 두는 것: 별로는 아니다. Spring 프로젝트에서 `JwtUtil`, `JwtProvider`를 `util` 패키지에 두는 것은 흔한 관행이다. 다만 `util`은 보통 상태 없는 정적 도우미를 뜻하는데 `JwtProvider`는 서명 키를 들고 있는 빈(`@Component`)이라는 차이가 있고, `JwtProvider`만 옮기면 `jwt/` 루트에 `JwtProperties`만 남아 어색했다.
- `JwtProperties`를 `config` 패키지에 두는 안(제안)과 `payload/dto`에 두는 안: `JwtProperties`는 yml 값을 담는 데이터 객체이고 필드만 가진 record로 로직이 없다. 사용자가 정한 `payload`의 정의(요청·응답뿐 아니라 데이터 객체를 모두 담는 패키지)에 비추면 `payload/dto`가 맞다. `jwt` 도메인이 자기 데이터 객체를 안에 모두 갖게 되어 `config` 패키지에 jwt 전용 클래스가 흩어지지 않는다. 앞서 "`JwtProperties`는 `jwt` 패키지에 둔다"고 한 결정을 이 구조로 갱신한다.
- "dto"는 보통 계층 사이에서 주고받는 객체를 뜻해서 설정 바인딩 객체는 이름이 조금 어색할 수 있다. 다만 `payload` 안에서 데이터 객체를 구분하는 용도로 쓰므로 문제 되지 않는다.

## 결정

- `JwtProvider`는 `jwt/v1/command/util`로 옮긴다.
- `JwtProperties`는 `jwt/v1/command/payload/dto`로 옮긴다.

## 이동 내용

| 클래스 | 이전 | 이후 |
|---|---|---|
| `JwtProvider` | `jwt/` | `jwt/v1/command/util` |
| `JwtProperties` | `jwt/` | `jwt/v1/command/payload/dto` |
| `JwtProviderTest` | `test/.../jwt/` | `test/.../jwt/v1/command/util` |

- 패키지 선언과 import를 모두 고쳤다. 같은 패키지에서 import 없이 쓰던 `JwtProvider`의 `JwtProperties`에는 import를 추가했다.
- `SecurityConfig`, `JwtCommandHandler`, `MemberCommandService`, `JwtAuthenticationFilter`와 테스트(`MemberReissueTest`, `JwtAuthenticationFilterTest`, `LoginTest`, `LogoutTest`)의 import를 새 위치로 맞췄다.
- 코드와 문서(`docs`)에서 옛 경로(`api.jwt.JwtProvider`, `api.jwt.JwtProperties`)는 남아 있지 않다. 동작은 바뀌지 않았다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 83개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 최종 `jwt` 구조

```
jwt/v1/command/
├── handler/JwtCommandHandler
├── repository/JwtRepository
├── util/JwtProvider
└── payload/
    ├── dto/   JwtClaims, IssuedToken, IssuedTokens, JwtProperties
    └── type/  TokenType
```

- 앞의 "데이터 객체 위치와 핸들러 이름 변경" 섹션과 plan4.md에는 `JwtProvider`, `JwtProperties`가 `jwt/` 루트에 있다고 적혀 있다. 히스토리라서 수정하지 않았고, 이 섹션이 최종 결정이다.
- 이 변경은 done1.md의 "최종 결과"와 README에는 이미 반영되어 있다.
