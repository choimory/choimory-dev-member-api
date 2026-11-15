# 목차

- [개요](#개요)
- [3단계 계획](#3단계-계획)
  - [답변](#답변)
  - [목표](#목표)
  - [Redis 설정](#redis-설정)
  - [auth 도메인](#auth-도메인)
  - [인증 (회원 조회)](#인증-회원-조회)
  - [로그인 필터](#로그인-필터)
  - [OpenAPI 명세서](#openapi-명세서)
  - [테스트](#테스트)
  - [영향과 주의](#영향과-주의)
  - [정해야 할 것](#정해야-할-것)
- [auth 도메인 대신 jwt와 security로 가는 안](#auth-도메인-대신-jwt와-security로-가는-안)
  - [답변](#답변-1)
  - [현재 auth의 성격](#현재-auth의-성격)
  - [옮기는 안](#옮기는-안)
  - [5단계 재발급 API](#5단계-재발급-api)
  - [장단점](#장단점)
  - [수정 범위](#수정-범위)
  - [결정 필요 사항](#결정-필요-사항)
- [발급, 재발급 컨트롤러를 jwt 또는 member 도메인에 두는 안](#발급-재발급-컨트롤러를-jwt-또는-member-도메인에-두는-안)
  - [답변](#답변-2)
  - [재발급을 컨트롤러로 두면 좋은 이유](#재발급을-컨트롤러로-두면-좋은-이유)
  - [어디에 둘까](#어디에-둘까)
  - [같이 정해야 할 점](#같이-정해야-할-점)
  - [변경 범위](#변경-범위)
  - [결정 필요 사항 2](#결정-필요-사항-2)
- [jwt 도메인 전환 결정](#jwt-도메인-전환-결정)
  - [결정](#결정)
  - [구조](#구조)
  - [로그인 흐름 변경](#로그인-흐름-변경)
  - [재발급 흐름](#재발급-흐름)
  - [함께 고치는 것](#함께-고치는-것)
- [재발급을 Member 컨트롤러로 옮기는 안](#재발급을-member-컨트롤러로-옮기는-안)
  - [답변](#답변-3)
  - [왜 좋은가](#왜-좋은가)
  - [바뀌는 구조](#바뀌는-구조)
  - [짚어둘 점](#짚어둘-점)
  - [결정 필요 사항 3](#결정-필요-사항-3)
- [토큰 발급과 재발급 호출 방식 결정](#토큰-발급과-재발급-호출-방식-결정)
  - [결정 내용](#결정-내용)
  - [반영 후 구조](#반영-후-구조)
  - [해석과 미정 사항](#해석과-미정-사항)

---

# 개요

- plan2.md의 설계 3단계: Redis 설정과 로그인(토큰 발급)을 구현한다.
- 1단계(Security 기본 설정)와 2단계(JWT)는 task2.md에 기록되어 있다.
- 로그아웃은 4단계, 토큰 재발급은 5단계에서 한다.
- 참고: plan2.md, task2.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# 3단계 계획

> 3단계 계획 ㄱㄱ

## 답변

3단계(Redis 설정과 로그인) 계획이다. 이 시점에는 코드를 작성하지 않았다.

## 목표

`POST /v1/auth/login`으로 로그인하면 Access/Refresh Token을 발급하고, Refresh Token을 Redis에 저장한다. 로그아웃과 재발급은 4·5단계 몫이다.

## Redis 설정

- `application-redis.yml`을 `datasource`, `jwt`처럼 프로필로 분리하고 `application.yml`의 `include`에 추가한다.
- `spring.data.redis.host/port/password`는 환경변수 `redis-host`, `redis-port`, `redis-password`로 받는다.
- 테스트는 `TestcontainersConfiguration`에 Redis 컨테이너(`redis:7`, `@ServiceConnection(name = "redis")`)를 추가한다. 이 설정을 import하는 기존 테스트도 Redis 컨테이너를 같이 띄우므로 테스트 시간이 조금 늘어난다.

## auth 도메인

`dev.choimory.member.api.auth.v1.command` 아래에 둔다.

- `AuthTokenRepository` (`repository`): `StringRedisTemplate`로 Refresh Token을 저장, 조회, 삭제한다.
  - 키는 `refresh:{memberId}:{jti}`이고 TTL은 토큰 만료까지 남은 시간이다.
  - 값은 의미 없는 표식(`"1"`)이면 충분하다. 토큰 자체는 서명과 jti로 검증하기 때문이다.
  - 로그인할 때마다 새 jti로 저장되므로 다중 로그인이 된다.
- `AuthCommandHandler` (`handler`): `JwtProvider`로 Access/Refresh Token을 발급하고 Refresh Token을 Redis에 저장한다.
- `AuthCommandService` (`service`): `issueTokens(memberId, 권한 코드)`가 핸들러를 호출해 `LoginResponse`를 반환한다. DB 접근이 없으므로 `@Transactional`은 필요 없다.
- `LoginResponse` (`payload/response`): `accessToken`, `refreshToken`, `tokenType`("Bearer"), `accessExpiresIn`, `refreshExpiresIn`(초)

## 인증 (회원 조회)

- `MemberRepository.findByEmail`: 권한을 `@EntityGraph`로 함께 가져온다. soft delete된 회원은 `@SQLRestriction`으로 자동 제외된다.
- `MemberCommandHandler.findByEmail`: `MemberDto.fromWithoutChildren`에 권한만 채워서 반환한다. `MemberDto.from`은 이미지, 정지 이력, 약관까지 지연 로딩하므로 쓰지 않는다.
- `MemberUserDetailsService` (`security/user`): 이메일로 회원을 조회해 `AuthenticatedMember`를 만든다. 상태가 `ACTIVATE`인 회원만 활성(`enabled`)이다. 정지·비활성·가입 대기 회원은 인증 단계에서 같은 실패로 처리된다.
- `AuthenticatedMember`에 이메일, 비밀번호(해시), `enabled`와 `from(MemberDto)` 팩토리를 추가한다.

## 로그인 필터

- `LoginFilter` (`security/filter`): `POST /v1/auth/login`의 `{email, password}` JSON을 받아 `AuthenticationManager`로 인증한다.
  - plan2.md에는 `UsernamePasswordAuthenticationFilter` 상속이라고 적었는데, 이 클래스는 폼 파라미터 기반이라 JSON을 받으려면 `AbstractAuthenticationProcessingFilter`를 직접 상속하는 쪽이 단순해서 이 방식으로 바꾸려 한다.
  - 스프링 빈이 아니라 `SecurityConfig`에서 직접 생성한다. JWT 필터와 같은 이유로, 빈으로 등록하면 서블릿 필터로 한 번 더 등록되기 때문이다.
- `LoginSuccessHandler`: `AuthCommandService.issueTokens`를 호출해 `CommonResponse<LoginResponse>` 200으로 응답한다.
- `LoginFailureHandler`: 어떤 인증 실패든(없는 이메일, 틀린 비밀번호, 정지 회원 등) 같은 401을 `SecurityResponseWriter`로 응답한다. 실패 사유는 노출하지 않는다.
- `SecurityConfig`
  - `DaoAuthenticationProvider`(`MemberUserDetailsService` + `PasswordEncoder`)로 `AuthenticationManager`를 구성한다.
  - `LoginFilter`를 `JwtAuthenticationFilter` 뒤에 연결한다.
  - `POST /v1/auth/login`을 `permitAll`에 추가한다.

## OpenAPI 명세서

`docs/api/v1/choimory-dev-member-api-auth-v1.json`을 새로 만들어 로그인 API(요청 예시, 200·400·401 응답 예시)를 작성한다. 재발급과 로그아웃은 해당 단계에서 추가한다.

## 테스트

- `AuthTokenRepository` 통합 테스트: 저장, 존재 확인, 삭제, TTL 설정
- `LoginTest` (Testcontainers Postgres + Redis)
  - 로그인 성공: 두 토큰이 내려오고, Access Token의 회원 ID와 권한이 맞고, Redis에 `refresh:{id}:{jti}` 키가 TTL 약 14일로 저장됨
  - 다중 로그인: 두 번 로그인하면 키가 2개
  - 실패: 틀린 비밀번호, 없는 이메일, 정지·비활성·가입 대기 회원, 탈퇴(soft delete) 회원 모두 같은 401
  - 잘못된 요청(JSON 아님, 빈 값)은 400
  - 로그인으로 받은 Access Token으로 보호된 테스트 주소 호출 성공
- 기존 테스트는 Redis 컨테이너가 추가되는 것 외에 변경 없다.

## 영향과 주의

- 새 파일은 대략 main 12개, test 3~4개, 설정·명세서 2개이다.
- 로컬 실행에 `redis-host` 등 환경변수가 추가로 필요하다.
- 앱 시작 시 나오던 `Using generated security password` 로그는 `MemberUserDetailsService`가 생기면 사라진다.

## 정해야 할 것

1. 로그인 요청이 JSON이 아니거나 `email`·`password`가 비어 있으면 400(추천)으로 할지, 인증 실패와 같은 401로 할지
2. Redis 값은 표식 `"1"`(추천)으로 할지, 토큰 해시를 저장할지
3. 로그인 응답 형태(`accessToken`, `refreshToken`, `tokenType`, `accessExpiresIn`, `refreshExpiresIn`)와 `LoginFilter`를 `AbstractAuthenticationProcessingFilter` 기반으로 바꾸는 것에 동의하는지
4. 이번 단계에 `auth` OpenAPI 명세서를 같이 작성할지
5. 아직 미정인 서명 키 하나/둘 결정 (추천: 하나)

---

# auth 도메인 대신 jwt와 security로 가는 안

> auth라는 도메인 대신 jwt랑 security로 가면 안되나?
>
> 이를태면 auth handler, repo는 거의 jwt 작업이고
> login request, login response는 거의 security filter 작업인데

## 답변

가능하다. `auth` 도메인의 내용이 사실상 토큰 처리와 필터 처리이다. 이 시점에는 코드를 수정하지 않았다.

## 현재 auth의 성격

| 지금 (`auth` 도메인) | 실제 성격 |
|---|---|
| `AuthTokenRepository` (Redis에 Refresh Token 저장·삭제, 4단계에서 블랙리스트 추가) | 토큰 저장소, 즉 JWT 작업 |
| `AuthCommandHandler` (토큰 발급 + 저장) | JWT 작업 |
| `AuthCommandService` (핸들러 한 줄 호출) | 필터와 핸들러 사이의 얇은 중간 단계 |
| `LoginRequest`, `LoginResponse` | 로그인 필터만 쓰는 객체 |

## 옮기는 안

| 현재 | 이후 |
|---|---|
| `AuthTokenRepository` | `jwt/JwtTokenRepository` (Refresh Token, 4단계의 블랙리스트) |
| `AuthCommandHandler` | `jwt/JwtTokenManager` (발급과 저장, 이후 재발급·폐기도 담당) |
| `AuthCommandService` | 삭제. `LoginSuccessHandler`가 `JwtTokenManager`를 직접 호출 |
| `LoginRequest`, `LoginResponse` | `security/payload/request`, `security/payload/response` |
| 테스트 | `AuthTokenRepositoryTest` → `jwt/JwtTokenRepositoryTest`, `LoginTest` → `security/LoginTest` |

`auth` 패키지가 사라지고 의존 방향은 `security → jwt`로 단순해진다.

## 5단계 재발급 API

로그인과 로그아웃은 필터라서 `auth`가 없어도 되지만, 재발급은 일반 컨트롤러로 계획했었다(`auth`의 `controller → service → handler`). 선택지는 다음과 같다.

1. 재발급도 필터로(`ReissueFilter`, `POST /v1/auth/reissue`): 토큰 관련 API 3개(로그인·로그아웃·재발급)가 모두 `security` 필터가 되어 `auth` 도메인이 필요 없다. 추천했다.
2. 재발급만 컨트롤러로 두기: 얇은 컨트롤러와 서비스를 둘 곳이 필요해서 `auth` 도메인을 결국 만들게 된다.

## 장단점

- 장점: 토큰 로직이 `jwt`와 `security` 두 곳에 모여 흐름이 짧아지고, 한 줄짜리 서비스 계층이 없어진다.
- 단점: 도메인 API는 `Controller → Service → Handler → Repository`라는 컨벤션을 따르는데, 이 부분은 필터에서 `jwt` 컴포넌트를 직접 호출하는 예외가 된다. 컨벤션의 레이어 규칙은 컨트롤러를 거치는 도메인 API를 위한 것이라 필터에는 적용되지 않는다고 보면 된다.
- 나중에 "내 로그인 기기 목록 조회"나 "전체 로그아웃" 같은 일반 API가 생기면 그때 `auth` 도메인을 만들면 된다. 지금은 필요하지 않다.

## 수정 범위

- 이동과 이름 변경은 6~7개 파일이고, 패키지와 import를 고치고 `clean build`로 확인한다.
- 기록 문서의 `auth` 경로(`plan2.md` 구조도, `plan3.md`, `task3.md`)도 같이 고쳐야 한다.
- OpenAPI의 `auth` 파일은 API 묶음 이름이라 그대로 둔다.

## 결정 필요 사항

1. `auth` 도메인을 없애고 위 표대로 옮길지
2. 5단계 재발급은 필터(추천)로 할지
3. 클래스 이름(`JwtTokenManager`, `JwtTokenRepository`)은 이대로 괜찮은지

---

# 발급, 재발급 컨트롤러를 jwt 또는 member 도메인에 두는 안

> 토큰 발급/재발급은 jwt 도메인이나 member 도메인에서 컨트롤러 만들면 되잖아?

## 답변

가능하다. 재발급은 필터보다 컨트롤러가 낫다는 점에서 이전 추천(재발급도 필터)을 바꿨다. 이 시점에는 코드를 수정하지 않았다.

## 재발급을 컨트롤러로 두면 좋은 이유

- 토큰 오류(`TOKEN_EXPIRED`, `INVALID_TOKEN`)가 `CommonException`으로 던져지면 `CommonExceptionHandler`가 그대로 처리한다. 필터로 하면 응답을 직접 작성해야 한다.
- 요청 본문(`{refreshToken}`)을 `@Valid`로 검증할 수 있다.
- 컨벤션(`Controller → Service → Handler → Repository`)을 그대로 따른다.

토큰 "발급"에 해당하는 로그인은 앞서 정한 대로 필터였다. 필터가 토큰을 발급할 때는 컨트롤러가 아니라 서비스 계층을 호출한다고 설명했다.

## 어디에 둘까

`jwt` 도메인(추천)

- 패키지는 `jwt/v1/command/{controller,service,handler,repository}`이고, 지금의 `auth`를 `jwt`로 이름만 바꾸는 모양이다. 컨벤션 명명대로 `JwtCommandController`, `JwtCommandService`, `JwtCommandHandler`, `JwtRepository`가 된다.
- 재발급은 `JwtCommandController`가 처리하고, 로그인·로그아웃 필터는 `JwtCommandService`를 호출한다.
- `LoginRequest`, `LoginResponse`는 필터 전용이므로 `security/payload`로 옮긴다.
- Redis 저장소가 `jwt` 도메인에 있으므로 Handler의 "자기 도메인 Repository만" 규칙도 지켜진다.

`member` 도메인(비추천)

- 토큰 발급·재발급은 회원 데이터와 관계가 없고, `MemberCommandHandler`는 `jwt`의 Redis 저장소를 직접 쓸 수 없어서 Service가 다른 도메인 Handler를 거쳐야 한다. `member`가 토큰 책임까지 지게 되어 비대해진다.

## 같이 정해야 할 점

- URL: README의 컨벤션은 컨트롤러 경로가 `/v1/{domain}`이다. `jwt` 도메인이면 `/v1/jwt/reissue`가 되는데, 로그인은 `/v1/auth/login`이다. `/v1/auth/*`로 통일하고 컨벤션에서 벗어나는 것을 감수할지, `/v1/jwt/*`로 바꿀지 정해야 한다. `/v1/auth/*` 유지를 추천했다. 클라이언트 입장에서 `jwt`는 구현 용어이기 때문이다.
- 이 경우 OpenAPI 파일은 기존 `choimory-dev-member-api-auth-v1.json`을 그대로 쓸 수 있다.

## 변경 범위

- 패키지 이름 변경 `auth` → `jwt`(`v1/command`), `LoginRequest`와 `LoginResponse`를 `security/payload`로 이동한다.
- 기록 문서(`plan2.md` 구조도, `plan3.md`, `task3.md`)의 경로도 같이 고친다.
- `JwtProvider`, `JwtProperties` 등 기존 `jwt` 패키지 루트의 클래스는 그대로 두므로 `jwt` 패키지에 인프라 클래스와 도메인 계층이 함께 있게 된다.

## 결정 필요 사항 2

1. `auth`를 `jwt` 도메인으로 바꾸고 재발급은 컨트롤러로 하는 안으로 갈지
2. URL은 `/v1/auth/*` 유지(추천)로 할지, `/v1/jwt/*`로 할지
3. 클래스 이름(`JwtCommandController/Service/Handler`, `JwtRepository`)은 괜찮은지

---

# jwt 도메인 전환 결정

> 1.jwt 도메인으로 바꾸고 발급, 재발급은 컨트롤러로
> 2. url도  auth 말고 jwt로
> 3.좋음 그렇게 해줘

## 결정

- `auth` 도메인을 `jwt` 도메인으로 바꾼다.
- 토큰 발급(로그인)과 재발급은 모두 `jwt` 도메인의 컨트롤러로 만든다. 로그인은 필터에서 컨트롤러로 바뀐다.
- URL은 `/v1/jwt/login`, `/v1/jwt/reissue`이다.
- 클래스 이름은 `JwtCommandController`, `JwtCommandService`, `JwtCommandHandler`, `JwtRepository`이다.
- 로그아웃은 언급이 없어서 4단계에서 따로 정한다. 필터(`logout()` 설정)로 갈지, 같은 도메인의 컨트롤러로 갈지 정해야 한다.

## 구조

`dev.choimory.member.api.jwt.v1.command` 아래에 `controller`, `service`, `handler`, `repository`, `payload`를 둔다. 기존 `jwt` 패키지 루트의 `JwtProvider`, `JwtProperties`, `TokenType`, `IssuedToken(s)`, `JwtClaims`는 그대로 둔다.

- `JwtCommandController` (`@RequestMapping("/v1/jwt")`): `POST /login`, `POST /reissue`
- `JwtCommandService`: 로그인과 재발급의 전체 흐름
- `JwtCommandHandler`: 인증, 토큰 발급과 저장, Refresh Token 사용 처리(검증과 삭제)
- `JwtRepository`: Redis의 Refresh Token 저장, 조회, 삭제 (기존 `AuthTokenRepository`)
- `payload`: `LoginRequest`, `ReissueRequest`, 두 API가 같은 모양으로 쓰는 `JwtTokenResponse`

## 로그인 흐름 변경

- `LoginFilter`, `LoginSuccessHandler`, `LoginFailureHandler`를 삭제한다.
- 컨트롤러가 `@Valid`로 `email`, `password`가 비어 있는지 검증하고, 서비스가 `AuthenticationManager`로 인증한 뒤 토큰을 발급한다.
- 인증 실패(`AuthenticationException`)는 기존 `CommonExceptionHandler`가 같은 401로 변환한다. 계정 정보 노출 방지는 그대로 유지된다.
- 빈 값은 `@Valid`가 400과 함께 실패한 필드 목록을 내려준다. 이전 필터 방식은 data 없는 400이었다.
- JSON이 아닌 본문(`HttpMessageNotReadableException`)은 현재 `CommonExceptionHandler`에 처리가 없어서 일반 예외 핸들러에 걸려 500이 된다. 회원가입 API도 같은 문제이므로 400 처리를 추가한다.
- `SecurityConfig`: 로그인 필터 연결과 관련 의존성을 걷어내고 `POST /v1/jwt/login`, `POST /v1/jwt/reissue`를 `permitAll`에 추가한다. `AuthenticationManager` 빈은 그대로 둔다.

## 재발급 흐름

1. Refresh Token을 검증하고(서명, 발급자, 만료, 종류) Redis에서 삭제한다. 삭제에 성공해야 사용할 수 있으므로 같은 Refresh Token을 두 번 쓰는 것을 막는다.
2. 회원을 다시 조회해 `ACTIVATE` 상태인지 확인하고 현재 권한을 읽는다. Refresh Token에는 권한이 없으므로 새 Access Token에 최신 권한이 반영된다.
3. 새 Access Token과 Refresh Token을 발급하고 새 Refresh Token을 Redis에 저장한다. (rotation)
4. 만료는 `TOKEN_EXPIRED`, 서명 오류·Redis에 없음·이미 사용함·정지 회원 등은 `INVALID_TOKEN`으로 401을 응답한다.

## 함께 고치는 것

- OpenAPI: `choimory-dev-member-api-auth-v1.json`을 도메인 이름에 맞춰 `choimory-dev-member-api-jwt-v1.json`으로 바꾸고 URL과 응답 예시를 고친다.
- 테스트: `AuthTokenRepositoryTest`, `LoginTest`를 `jwt` 도메인으로 옮겨 고치고, 재발급 테스트를 새로 작성한다.
- 기록 문서의 `auth` 경로는 작업을 마친 뒤 말씀하실 때 고친다.

---

# 재발급을 Member 컨트롤러로 옮기는 안

> jwt 재발급도 Membercontroller쪽으로 옮기고, jwt 핸들러~리포지토리만 남기면 안돼? memberservice에서 jwt handler, repository 호출하는 식으로

## 답변

가능하고 레이어 규칙에도 맞다. 컨벤션이 "Service는 다른 도메인의 Handler도 호출할 수 있다"고 하기 때문이다. 의존 방향도 지금보다 깔끔해진다. 이 시점에는 코드를 수정하지 않았다.

## 왜 좋은가

지금은 `JwtCommandService`가 재발급 중에 회원 상태와 권한을 읽으려고 `MemberCommandHandler`를 호출한다(`jwt` → `member` 의존). `MemberCommandService`로 옮기면 회원 조회는 자기 도메인 안에서 끝나고, 토큰 발급·검증·저장만 `JwtCommandHandler`에 맡긴다(`member` → `jwt` 의존). `jwt`는 토큰 처리만 하는 하위 모듈이 되어 다른 도메인도 재사용하기 쉽다.

## 바뀌는 구조

| 현재 | 이후 |
|---|---|
| `JwtCommandController` `/v1/jwt/reissue` | `MemberCommandController`의 `reissue` |
| `JwtCommandService` (`issueTokens`, `reissue`) | 삭제. 로직은 `MemberCommandService`로 이동 |
| `JwtCommandHandler`, `JwtRepository` | 유지 |
| `LoginSuccessHandler` → `JwtCommandService.issueTokens` | `MemberCommandService.issueTokens` 호출 |
| 4단계 로그아웃 | `MemberCommandService.logout`이 `JwtCommandHandler`를 호출 |

`jwt` 패키지는 루트 인프라(`JwtProvider` 등)와 `v1/command/{handler,repository}`만 남는다.

## 짚어둘 점

1. `Service`가 `JwtRepository`를 직접 부르지 않는 것을 추천했다. 컨벤션은 `Service → Handler → Repository`이고, `JwtCommandHandler`가 이미 `JwtRepository`를 감싸고 있어서 `MemberCommandService`는 `JwtCommandHandler`만 호출하면 된다. "jwt handler, repository 호출"은 이 흐름(핸들러를 거쳐 리포지토리 접근)으로 이해했다.
2. 재발급 URL이 바뀐다. `MemberCommandController`의 경로가 `/v1/member`라서 `/v1/jwt/reissue`가 `/v1/member/reissue`가 된다. `/v1/reissue`로 하려면 별도 컨트롤러 클래스가 필요하다.
3. `MemberCommandService`가 커진다. 가입, 토큰 발급, 재발급, 로그아웃이 모인다. 당장은 괜찮지만 토큰 관련 로직이 더 늘면 분리를 다시 고려해야 한다.
4. 요청·응답 객체 위치: `ReissueRequest`, `JwtTokenResponse`는 `member` 컨트롤러가 쓰므로 `member/v1/command/payload`로 옮기고 이름을 규칙에 맞추는 안이다(`MemberReissueRequest`, `MemberTokenResponse`). 이름은 정해야 한다.
5. OpenAPI: 재발급은 `member` 문서로 옮기고, `jwt` 문서에는 로그인(과 이후 로그아웃)만 남긴다. 파일 이름은 그대로 두어도 된다.
6. 테스트: `ReissueTest`의 경로를 바꾸고, `JwtCommandService`를 직접 쓰는 곳이 없는지 정리한다. 규모는 파일 7~8개 안팎이다.

## 결정 필요 사항 3

1. 재발급 URL을 `/v1/member/reissue`로 할지
2. 호출 흐름을 `MemberCommandService` → `JwtCommandHandler` → `JwtRepository`로 할지
3. 요청·응답 객체 이름을 `MemberReissueRequest`, `MemberTokenResponse`로 할지
4. OpenAPI 정리 방식에 동의하는지

---

# 토큰 발급과 재발급 호출 방식 결정

> 토큰발급(로그인)은 로그인 필터에서 jwt handler 호출해서 발급하고, 토큰 재발급은 membercontroller에서 jwt handler 호출해서 재발급하는식으로 가자 지금 내용 plan3에 추가

## 결정 내용

- 토큰 발급(로그인)은 로그인 필터가 `JwtCommandHandler`를 호출해서 발급한다.
- 토큰 재발급은 `MemberCommandController`에서 `JwtCommandHandler`를 호출해서 재발급한다.
- 이 시점에는 코드를 수정하지 않았다.

## 반영 후 구조

| 기능 | 처리 |
|---|---|
| 로그인 `POST /v1/login` | `LoginFilter` → `LoginSuccessHandler` → `JwtCommandHandler.issueTokens` |
| 재발급 | `MemberCommandController.reissue` → `MemberCommandService.reissue` → `MemberCommandHandler.findById`(회원 상태와 권한 조회), `JwtCommandHandler`(Refresh Token 검증·사용 처리, 새 토큰 발급·저장) |
| 로그아웃 (4단계) | 로그아웃 필터와 핸들러가 `JwtCommandHandler`를 호출하는 방향이다. 요청 방식은 시작 전에 확인한다. |

- `JwtCommandController`, `JwtCommandService`는 삭제한다. `jwt` 패키지에는 `JwtProvider` 등 루트 인프라와 `v1/command/{handler,repository}`만 남는다.
- 로그인 필터가 서비스를 거치지 않고 핸들러를 직접 호출하는 것은 컨벤션(핸들러는 서비스에서만 호출한다)의 예외이다. 필터는 컨트롤러·서비스 계층 밖이라는 판단이다. 로그인 성공 핸들러가 `JwtCommandHandler.issueTokens`가 반환한 토큰을 응답 객체로 변환한다.
- 재발급은 컨트롤러를 거치므로 `Controller → Service → Handler` 흐름을 그대로 따른다. 회원 조회는 같은 `member` 도메인의 핸들러이고, `jwt`는 토큰 처리 핸들러만 호출한다.

## 해석과 미정 사항

앞의 "결정 필요 사항 3" 중 아래는 명시적인 답이 없어서 다음과 같이 전제한다. 다르면 정정한다.

1. 재발급 URL은 `MemberCommandController`로 옮기므로 `/v1/member/reissue`가 된다.
2. 호출 흐름은 `MemberCommandService` → `JwtCommandHandler` → `JwtRepository`이다. 말씀한 "jwt handler 호출"에 맞춰 서비스가 리포지토리를 직접 부르지 않는다.
3. 요청·응답 객체 이름(`MemberReissueRequest`, `MemberTokenResponse` 등)은 정하지 않았다. 로그인 응답도 같은 객체를 쓰므로 함께 정해야 한다.
4. OpenAPI: 재발급을 `member` 문서로 옮기고 `jwt` 문서에는 로그인만 남기는 정리가 필요한지 정하지 않았다.
5. 로그인 필터가 `JwtCommandHandler`를 직접 호출할 때, 토큰 발급 결과를 응답 객체로 변환하는 위치(성공 핸들러 안)는 구현 시 확정한다.
