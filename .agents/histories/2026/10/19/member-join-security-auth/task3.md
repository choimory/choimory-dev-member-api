# 목차

- [개요](#개요)
- [Redis 설정과 로그인 (3단계)](#redis-설정과-로그인-3단계)
  - [답변](#답변)
  - [확정한 결정](#확정한-결정)
  - [작업 내용](#작업-내용)
  - [계획과 달라진 점](#계획과-달라진-점)
  - [테스트](#테스트)
  - [검증 결과](#검증-결과)
  - [참고](#참고)
- [Redis 설정에 username 추가](#redis-설정에-username-추가)
  - [답변](#답변-1)
  - [작업 내용](#작업-내용-1)
  - [확인하지 못한 것](#확인하지-못한-것)
  - [남은 사항](#남은-사항)
- [auth 도메인을 jwt 도메인으로 전환](#auth-도메인을-jwt-도메인으로-전환)
  - [답변](#답변-2)
  - [결정](#결정)
  - [구조](#구조)
  - [로그인 흐름 변경](#로그인-흐름-변경)
  - [재발급](#재발급)
  - [삭제한 것](#삭제한-것)
  - [찾아서 고친 문제](#찾아서-고친-문제)
  - [테스트](#테스트-1)
  - [OpenAPI](#openapi)
  - [검증 결과](#검증-결과-1)
  - [참고](#참고-1)
  - [남은 사항](#남은-사항-1)
- [로그인 필터 복구](#로그인-필터-복구)
  - [답변](#답변-3)
  - [해석 정정](#해석-정정)
  - [최종 구조](#최종-구조)
  - [복구 내용](#복구-내용)
  - [테스트](#테스트-2)
  - [OpenAPI](#openapi-1)
  - [검증 결과](#검증-결과-2)
  - [참고](#참고-2)
  - [남은 사항](#남은-사항-2)
- [회원가입 API 명세서 400 예시 추가](#회원가입-api-명세서-400-예시-추가)
  - [답변](#답변-4)
  - [현재 반영 상태](#현재-반영-상태)
  - [추가한 내용](#추가한-내용)
  - [검증](#검증)
- [로그인 주소를 /v1/login으로 변경](#로그인-주소를-v1login으로-변경)
  - [답변](#답변-5)
  - [결정](#결정-1)
  - [변경 내용](#변경-내용)
  - [검증 결과](#검증-결과-3)
  - [남은 사항](#남은-사항-3)
- [재발급을 Member 컨트롤러로 이동](#재발급을-member-컨트롤러로-이동)
  - [답변](#답변-6)
  - [결정 내용](#결정-내용)
  - [반영된 구조](#반영된-구조)
  - [코드 변경](#코드-변경)
  - [테스트](#테스트-3)
  - [OpenAPI](#openapi-2)
  - [검증 결과](#검증-결과-4)
  - [참고](#참고-3)
  - [남은 사항](#남은-사항-4)

---

# 개요

- plan3.md에서 계획한 3단계(Redis 설정과 로그인)를 구현한다. `POST /v1/auth/login`으로 로그인하면 Access/Refresh Token을 발급하고 Refresh Token을 Redis에 저장한다.
- 1단계(Security 기본 설정)와 2단계(JWT)는 task2.md에 기록되어 있다.
- 로그아웃은 4단계, 토큰 재발급은 5단계에서 한다.
- 참고: plan2.md, plan3.md, task2.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# Redis 설정과 로그인 (3단계)

> 추천안대로 3단계 시작해줘

## 답변

plan3.md의 "정해야 할 것" 5가지를 추천안으로 확정하고, 3단계를 구현했다.

## 확정한 결정

| # | 항목 | 결정 |
|---|---|---|
| 1 | 잘못된 로그인 요청 | JSON이 아니거나 `email`·`password`가 비어 있으면 400 |
| 2 | Redis 값 | 표식 `"1"` (토큰 자체는 서명과 jti로 검증) |
| 3 | 로그인 응답과 필터 | `accessToken`, `refreshToken`, `tokenType`, `accessExpiresIn`, `refreshExpiresIn`, 필터는 `AbstractAuthenticationProcessingFilter` 기반 |
| 4 | OpenAPI | 이번 단계에 `auth` 명세서를 같이 작성 |
| 5 | 서명 키 | 하나(`jwt-secret`)로 유지 |

## 작업 내용

### Redis 설정

- `application-redis.yml`(신규): `datasource`, `jwt`처럼 프로필로 분리했고 `application.yml`의 `spring.profiles.include`를 `datasource, jwt, redis`로 바꿨다.
  - `spring.data.redis.host: ${redis-host}`, `port: ${redis-port}`, `password: ${redis-password:}`
- 테스트의 `TestcontainersConfiguration`에 Redis 컨테이너(`redis:7`, `@ServiceConnection(name = "redis")`)를 추가했다. 이 설정을 import하는 기존 테스트도 Redis 컨테이너를 같이 띄운다.

### auth 도메인 (`dev.choimory.member.api.auth.v1.command`)

- `AuthTokenRepository` (`repository`): `StringRedisTemplate`로 Refresh Token을 저장, 조회, 삭제한다.
  - 키는 `refresh:{memberId}:{jti}`, 값은 표식 `"1"`, TTL은 토큰 만료까지 남은 시간이다.
  - 로그인할 때마다 새 jti로 저장되므로 기기별 다중 로그인이 된다.
- `AuthCommandHandler` (`handler`): `JwtProvider`로 Access/Refresh Token을 발급하고 Refresh Token을 Redis에 저장한다. `jwt/IssuedTokens`(Access와 Refresh 한 쌍)를 반환한다.
- `AuthCommandService` (`service`): `issueTokens(memberId, 권한 코드)`가 핸들러를 호출해 `LoginResponse`를 반환한다. DB 접근이 없으므로 `@Transactional`은 없다.
- `LoginRequest`, `LoginResponse` (`payload`): 응답에는 `accessToken`, `refreshToken`, `tokenType`("Bearer"), `accessExpiresIn`, `refreshExpiresIn`(초)이 담긴다.

### 회원 조회와 인증

- `MemberRepository.findByEmail`: 권한을 `@EntityGraph`로 함께 가져온다. soft delete된 회원은 `@SQLRestriction`으로 자동 제외된다.
- `MemberCommandHandler.findByEmail`: `MemberDto.fromWithoutChildren`에 권한만 채워서 반환한다. `MemberDto.from`은 이미지, 정지 이력, 약관까지 지연 로딩하므로 쓰지 않았다.
- `security/user/MemberUserDetailsService`: 이메일로 회원을 조회해 `AuthenticatedMember`를 만든다.
- `security/user/AuthenticatedMember`: 이메일, 비밀번호, `enabled`와 `from(MemberDto)` 팩토리, 토큰에 담을 `authorityCodes()`를 추가했다. 상태가 `ACTIVATE`인 회원만 활성이다.

### 로그인 필터

- `security/filter/LoginFilter`: `POST /v1/auth/login`의 JSON 본문을 읽어 `AuthenticationManager`로 인증한다. 요청 본문이 JSON이 아니거나 이메일, 비밀번호가 비어 있으면 인증을 시도하지 않고 400으로 응답한다. 스프링 빈이 아니라 `SecurityConfig`에서 직접 생성한다. JWT 필터와 같은 이유로, 빈으로 등록하면 서블릿 필터로 한 번 더 등록되기 때문이다.
- `security/handler/LoginSuccessHandler`: 인증된 회원의 토큰을 발급해 200 `CommonResponse<LoginResponse>`로 내려준다.
- `security/handler/LoginFailureHandler`: 없는 이메일, 틀린 비밀번호, 로그인할 수 없는 상태 등 모든 인증 실패를 같은 401로 응답한다. 계정 정보 노출을 막기 위해서이다.
- `security/handler/SecurityResponseWriter`: 데이터를 담는 `write(response, status, data)` 오버로드를 추가하고 내부 작성 메서드를 제네릭으로 바꿨다.
- `config/SecurityConfig`
  - `DaoAuthenticationProvider`(`MemberUserDetailsService` + `PasswordEncoder`)로 `AuthenticationManager` 빈을 만들었다. 없는 이메일은 비밀번호 오류와 같은 인증 실패로 처리되어 계정 존재 여부가 노출되지 않는다.
  - `POST /v1/auth/login`을 `permitAll`에 추가했다.
  - `LoginFilter`를 `JwtAuthenticationFilter` 뒤에 연결했다.

### OpenAPI 명세서

- `docs/api/v1/choimory-dev-member-api-auth-v1.json`(신규): 로그인 API의 요청 예시 1개, 응답 예시 200(토큰), 400(JSON이 아닌 본문, 비밀번호 누락), 401(인증 실패), 500(서버 오류)을 작성했다.
- 재발급과 로그아웃은 해당 단계에서 추가한다.
- JSON 파싱과 `$ref` 6개 해석을 파이썬으로 확인했다. OpenAPI 전용 검증기와 Apidog 가져오기는 확인하지 못했다.

## 계획과 달라진 점

- 계획대로 `LoginFilter`는 `AbstractAuthenticationProcessingFilter` 기반으로 만들었다. plan2.md에는 `UsernamePasswordAuthenticationFilter` 상속이라고 적혀 있었는데, 이 클래스는 폼 파라미터 기반이라 JSON을 받으려면 직접 상속하는 쪽이 단순하다.
- 테스트용 컨트롤러를 `TestSecurityController`로 분리했다. `LoginTest`에서 `/test/whoami`가 404였는데, Boot가 테스트 클래스의 중첩 클래스를 컴포넌트 스캔에서 제외하기 때문이었다. 처음에는 `JwtAuthenticationFilterTest`의 중첩 컨트롤러가 스캔된다고 가정했다가 틀렸다. 별도 클래스로 빼서 `JwtAuthenticationFilterTest`와 `LoginTest`가 `@Import`한다.
- `member-api/build.gradle`의 `test` 태스크에 시스템 프로퍼티 `redis-host`, `redis-port`를 추가했다. Redis 설정의 `${redis-port}`는 숫자라서, 값이 비어 있으면 Testcontainers가 연결을 덮어써도 바인딩이 실패할 수 있기 때문이다.

## 테스트

- `AuthTokenRepositoryTest`(신규, Redis): 저장, 존재 확인, 삭제, 다른 jti·다른 회원과의 구분, 14일 TTL
- `LoginTest`(신규, Postgres + Redis)
  - 로그인 성공: 두 토큰이 내려오고, Access Token의 회원 ID와 `MEMBER` 권한이 맞고, Refresh Token이 `refresh:{memberId}:{jti}` 키로 14일 안팎의 TTL로 저장됨
  - 다중 로그인: 같은 회원이 두 번 로그인하면 Refresh Token 키가 2개
  - 로그인으로 받은 Access Token으로 보호된 테스트 주소 호출 성공
  - 틀린 비밀번호와 없는 이메일이 완전히 같은 401 본문이고 Redis에 키가 생기지 않음
  - 가입 대기·비활성·정지 상태 회원은 올바른 비밀번호여도 401이고, `ACTIVATE`로 돌리면 로그인됨
  - 탈퇴(soft delete)한 회원은 401
  - 잘못된 본문 5종(JSON 아님, 빈 객체, 비밀번호 누락, 비밀번호 공백, 빈 본문)은 400
  - GET 요청은 401
- 기존 테스트는 Redis 컨테이너가 추가되는 것 외에 변경이 없다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 66개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과
- 앱 시작 때 나오던 `Using generated security password` 로그가 테스트 출력에서 사라진 것을 확인했다. `MemberUserDetailsService`가 생겼기 때문이다.

## 참고

- 로컬에서 실행하려면 환경변수 `jwt-secret`에 더해 `redis-host`, `redis-port`가 필요하고 로컬 Redis도 떠 있어야 한다. IntelliJ 실행 설정에 추가해야 한다.
- Redis에 연결할 수 없으면 로그인 때 500이 난다. OpenAPI에도 적었다.
- 서명 키는 하나로 유지했다.

---

# Redis 설정에 username 추가

> redis 설정에 username은 왜 없어

## 답변

특별한 이유 없이 비밀번호만 있는 기본 Redis 구성(`requirepass`)을 가정해서 빠뜨렸다. Redis 6 이상의 ACL로 사용자를 만들어 쓰는 환경(AWS ElastiCache의 RBAC, Redis Cloud, 직접 `ACL SETUSER`)에서는 `AUTH {username} {password}`로 접속해야 해서 username이 없으면 인증에 실패한다. 비밀번호만 쓰는 기본 구성은 내부적으로 `default` 사용자에 대한 AUTH라서 username이 필요 없다. Spring Boot는 `spring.data.redis.username`을 지원한다.

## 작업 내용

- `application-redis.yml`에 선택 값 `username: ${redis-username:}`를 추가했다. 환경변수 `redis-username`이 없으면 빈 값이라 비밀번호만 쓰는 구성과 같게 동작한다. 위에 "Redis ACL(6 이상)을 쓰는 환경에서만 필요하다"는 주석도 달았다.
- `RedisConnectionTest`(신규, Redis 컨테이너)를 만들었다. 다른 통합 테스트는 Testcontainers가 연결 정보를 덮어써서 설정값 자체를 쓰지 않으므로, 이 테스트는 `RedisAutoConfiguration`만 올리고 `username`과 `password`를 빈 값으로 두고 접속해 값을 쓰고 읽는다. 통과해서 빈 값이 안전하게 무시되는 것을 확인했다.
- `./gradlew clean build` 성공, `member-api` 테스트 67개, `member-domain` 테스트 107개 모두 성공

## 확인하지 못한 것

- 실제 ACL이 설정된 Redis에 `redis-username`과 `redis-password`를 넣어 접속하는 경우는 시험하지 못했다. 운영 Redis가 ACL을 쓴다면 해당 환경에서 확인해야 한다.

## 남은 사항

- 로컬 실행 환경변수: `jwt-secret`(32바이트 이상), `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`
- 4단계: `logout()` 설정, 핸들러, Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO)
- 5단계: 토큰 재발급 API (`POST /v1/auth/reissue`)
- `choimory-dev-member-api-auth-v1.json`에 로그아웃, 재발급 API 추가
- plan2.md에는 7가지 결정 사항이 아직 기록되어 있지 않고, task2.md에는 3단계를 따로 적지 않았다. README에는 반영하지 않았다.

---

# auth 도메인을 jwt 도메인으로 전환

> 1.jwt 도메인으로 바꾸고 발급, 재발급은 컨트롤러로
> 2. url도  auth 말고 jwt로
> 3.좋음 그렇게 해줘

## 답변

plan3.md에서 논의한 대로 `auth` 도메인을 `jwt` 도메인으로 바꾸고, 토큰 발급(로그인)과 재발급을 모두 `jwt` 도메인의 컨트롤러로 구현했다. 앞의 "Redis 설정과 로그인 (3단계)" 섹션은 전환 전의 `auth` 도메인과 로그인 필터 기준이므로, 현재 구조는 이 섹션을 따른다.

## 결정

- `auth` 도메인을 `jwt` 도메인으로 바꾼다.
- 토큰 발급(로그인)과 재발급은 모두 컨트롤러로 만든다. 로그인은 필터에서 컨트롤러로 바뀐다. "발급, 재발급은 컨트롤러로"를 로그인도 컨트롤러가 처리하는 것으로 해석했다.
- URL은 `/v1/jwt/login`, `/v1/jwt/reissue`이다.
- 클래스 이름은 `JwtCommandController`, `JwtCommandService`, `JwtCommandHandler`, `JwtRepository`이다.
- 로그아웃은 언급이 없어서 손대지 않았고 4단계에서 정한다.

## 구조

`dev.choimory.member.api.jwt.v1.command` 아래에 `controller`, `service`, `handler`, `repository`, `payload`를 두었다. 기존 `jwt` 패키지 루트의 `JwtProvider`, `JwtProperties`, `TokenType`, `IssuedToken(s)`, `JwtClaims`는 그대로 둔다.

- `JwtCommandController` (`@RequestMapping("/v1/jwt")`): `POST /login`, `POST /reissue`. 컨트롤러가 `CommonResponse`로 감싸 반환한다.
- `JwtCommandService`: 로그인과 재발급의 전체 흐름
- `JwtCommandHandler`: 인증(`authenticate`), 토큰 발급과 저장(`issueTokens`), Refresh Token 사용 처리(`consumeRefreshToken`)
- `JwtRepository`: Redis의 Refresh Token 저장, 조회, 삭제. 기존 `AuthTokenRepository`이며 `deleteRefreshToken`이 삭제 성공 여부를 반환하도록 바꿨다.
- `payload`: `LoginRequest`, `ReissueRequest`, 두 API가 같은 모양으로 쓰는 `JwtTokenResponse`(기존 `LoginResponse`). 컨벤션의 "API별 응답 객체"를 따르면 둘로 나눠야 하지만 같은 모양이라 하나로 통일했다.

## 로그인 흐름 변경

- 컨트롤러가 `@Valid`로 `email`, `password`가 비어 있는지 검증한다. 메시지는 "이메일을 입력해야 합니다.", "비밀번호를 입력해야 합니다."로 고정했다. 비밀번호 규칙이 노출되지 않도록 형식 검증은 하지 않는다.
- 서비스가 `AuthenticationManager`(핸들러의 `authenticate`)로 인증한 뒤 토큰을 발급한다.
- 인증 실패(`AuthenticationException`)는 기존 `CommonExceptionHandler`가 같은 401로 변환한다. 없는 이메일, 틀린 비밀번호, 로그인할 수 없는 상태 모두 같은 응답이라 계정 정보 노출 방지는 그대로이다.
- 빈 값은 400과 함께 실패한 필드 목록(`CommonValidateResult`)이 내려간다. 이전 필터 방식은 data 없는 400이었다.
- `SecurityConfig`: 로그인 필터 연결과 관련 의존성(성공·실패 핸들러, 응답 작성기, `ObjectMapper`)을 걷어냈다. `POST /v1/member/join`, `/v1/jwt/login`, `/v1/jwt/reissue`를 `permitAll`에 두고, `AuthenticationManager` 빈은 그대로 둔다.

## 재발급

`POST /v1/jwt/reissue` `{refreshToken}`

1. Refresh Token을 검증하고(서명, 발급자, 만료, 종류) Redis에서 삭제한다. 삭제에 성공한 요청만 통과하므로 같은 토큰으로 동시에 요청해도 하나만 성공한다.
2. 회원을 다시 조회해(`MemberCommandHandler.findById`) `ACTIVATE` 상태인지 확인하고 현재 권한을 읽는다. Refresh Token에는 권한이 없으므로 새 Access Token에 최신 권한이 반영된다.
3. 새 Access Token과 Refresh Token을 발급하고 새 Refresh Token을 Redis에 저장한다. (rotation)
4. 만료는 `TOKEN_EXPIRED`, 서명 오류·Access Token을 보냄·저장되지 않았거나 이미 사용한 토큰·재발급할 수 없는 회원(정지, 비활성, 탈퇴)은 `INVALID_TOKEN`으로 401을 응답한다. 둘 다 `CommonException`이라 `CommonExceptionHandler`가 처리한다.
5. 회원이 재발급할 수 없는 상태여도 이전 Refresh Token은 이미 사용 처리되어 삭제된 상태이고 새 Refresh Token은 저장되지 않는다.

관련 변경

- `MemberRepository.findWithAuthoritiesById`(권한 `@EntityGraph`), `MemberCommandHandler.findById`. 변환 코드는 `findByEmail`과 `toDtoWithAuthorities`로 공통화했다.
- `JwtProvider.invalidToken()`을 public static으로 바꿔 `JwtProvider`, `JwtCommandHandler`, `JwtCommandService`가 같은 예외를 쓴다.

## 삭제한 것

- `LoginFilter`, `LoginSuccessHandler`, `LoginFailureHandler`
- `SecurityResponseWriter`의 데이터를 담는 오버로드 (로그인 성공 핸들러 전용이었다)
- `auth` 패키지 전체 (main, test)

## 찾아서 고친 문제

JSON이 아닌 요청 본문(`HttpMessageNotReadableException`)은 `CommonExceptionHandler`에 처리가 없어 일반 예외로 빠져 500이 되었다. 회원가입 API도 같은 상태였다. 400 처리(`BAD_REQUEST`, data 없음)를 추가했다.

## 테스트

- `JwtRepositoryTest`(기존 `AuthTokenRepositoryTest`): 삭제가 성공 시 `true`, 이미 없으면 `false`를 반환하는지 포함해 갱신했다.
- `LoginTest`: URL을 `/v1/jwt/login`으로 바꾸고, 빈 값 400(필드 목록)과 JSON이 아닌 본문 400으로 나눠 갱신했다.
- `ReissueTest`(신규, Postgres + Redis)
  - rotation: 새 토큰이 이전과 달라야 하고, 이전 Refresh Token은 삭제되고 새 Refresh Token만 저장되며, 새 Access Token으로 보호된 주소를 호출할 수 있음
  - 한 번 쓴 Refresh Token의 재사용은 `INVALID_TOKEN`
  - 로그인 이후 `ADMIN` 권한이 추가된 회원은 재발급한 Access Token에 두 권한이 모두 있음
  - 로그인 후 정지된 회원과 탈퇴(soft delete)한 회원은 재발급 불가이고, Redis에 Refresh Token이 남지 않음
  - 만료된 Refresh Token은 `TOKEN_EXPIRED`, 잘못된 문자열·Access Token·저장된 적 없는 토큰은 `INVALID_TOKEN`
  - 빈 값은 400과 필드 목록, JSON이 아닌 본문은 400
  - 잘못된 Access Token을 `Authorization` 헤더에 담아 보내도 재발급이 처리되고, GET은 401
- `CommonExceptionHandlerTest`: JSON이 아닌 본문이 400인지 추가했다.
- 처음에 `JwtRepositoryTest`의 옛 `auth` import를 놓쳐 컴파일이 실패했다. 고쳐서 통과했다.

## OpenAPI

- `choimory-dev-member-api-auth-v1.json`을 `choimory-dev-member-api-jwt-v1.json`으로 이름을 바꿨다. 도메인 이름 규칙에 맞춘 것이다.
- 로그인(`/v1/jwt/login`)과 재발급(`/v1/jwt/reissue`)을 작성했다.
  - 로그인 응답 예시: 200, 400(필드 누락, 공백, JSON이 아닌 본문), 401, 500
  - 재발급 응답 예시: 200, 400(필드 누락, JSON이 아닌 본문), 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500
- JSON 파싱과 `$ref` 12개 해석을 파이썬으로 확인했다. 전용 검증기와 Apidog 가져오기는 확인하지 못했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 76개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 앞의 "Redis 설정과 로그인 (3단계)" 섹션의 `auth` 도메인, `LoginFilter`, `/v1/auth/login` 설명은 전환 전 기준이다. 히스토리라서 수정하지 않았다. plan2.md 구조도의 `auth` 도메인과 plan3.md 앞쪽 계획 섹션도 같다.
- 로컬 실행 환경변수는 그대로이다: `jwt-secret`(32바이트 이상), `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`

## 남은 사항

- 4단계: 로그아웃과 Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO). 로그아웃을 `logout()` 설정(필터)으로 할지, `jwt` 도메인 컨트롤러(`POST /v1/jwt/logout`)로 할지 정해야 한다.
- OpenAPI에 로그아웃 API 추가
- plan2.md 구조도, plan3.md, task3.md 앞쪽 섹션의 옛 `auth` 경로 정리 여부 결정
- README에는 반영하지 않았다.

---

# 로그인 필터 복구

> 로그인필터는 어디갔어?
>
> 로그인/로그아웃은 security에서 할거야. 로그인 필터랑 로그아웃 필터를 타되, 그 메소드에서 Jwt의 서비스나 핸들러를 사용하면 되는거 아냐?
>
> 네 그 계획대로 복구 시작해줘

## 답변

로그인 필터를 삭제한 것은 "발급, 재발급은 컨트롤러로"를 로그인까지 컨트롤러가 처리하는 것으로 잘못 해석했기 때문이다. 로그인과 로그아웃은 Security 필터가 요청을 받고, 토큰 발급·폐기는 `jwt` 도메인의 서비스를 호출하는 것이 원래 설계(plan2.md)였다. 삭제한 파일은 커밋 `3150f02`에서 가져와 복구했다.

## 해석 정정

앞의 "auth 도메인을 jwt 도메인으로 전환" 섹션과 plan3.md의 "jwt 도메인 전환 결정" 섹션에는 로그인을 `JwtCommandController`의 `/login`으로 처리하고 `LoginFilter`를 삭제했다고 적혀 있다. 이 부분은 이 섹션의 내용으로 대체된다. 이름을 `auth`에서 `jwt`로 바꾼 것, 재발급을 컨트롤러로 만든 것, URL을 `/v1/jwt/*`로 한 것은 그대로 유효하다.

## 최종 구조

| 기능 | 처리 위치 |
|---|---|
| 로그인 `POST /v1/jwt/login` | `security`의 `LoginFilter`. 성공 핸들러가 `JwtCommandService.issueTokens`를 호출한다. |
| 재발급 `POST /v1/jwt/reissue` | `jwt` 도메인의 `JwtCommandController` |
| 로그아웃 (4단계) | `security`의 필터(`logout()` 설정과 핸들러)에서 처리하고 `jwt` 서비스를 호출할 예정이다. |

## 복구 내용

### security

- `security/filter/LoginFilter`: 경로를 `/v1/jwt/login`으로 바꿔 복구했다. `AbstractAuthenticationProcessingFilter` 기반이며, 요청 본문이 JSON이 아니거나 이메일, 비밀번호가 비어 있으면 인증을 시도하지 않고 data 없는 400으로 응답한다.
- `security/handler/LoginSuccessHandler`: `AuthCommandService` 대신 `JwtCommandService.issueTokens`를 호출해 200 `CommonResponse<JwtTokenResponse>`로 응답한다.
- `security/handler/LoginFailureHandler`: 모든 인증 실패를 같은 401로 응답한다.
- `security/handler/SecurityResponseWriter`: 데이터를 담는 `write(response, status, data)` 오버로드를 되살렸다.
- `security/payload/request/LoginRequest`: 필터만 쓰는 객체라 `jwt` 도메인에서 `security/payload/request`로 옮겼다. 필터가 직접 검증하므로 `@NotBlank`는 뺐다.
- `config/SecurityConfig`: 로그인 필터와 성공·실패 핸들러, 응답 작성기, `ObjectMapper` 의존을 다시 연결했다. 로그인 필터는 `JwtAuthenticationFilter` 뒤에 직접 생성해 연결한다. `/v1/jwt/login`, `/v1/jwt/reissue`는 `permitAll` 그대로이다.

### jwt 도메인

- `JwtCommandController`: `/login`을 제거하고 재발급만 남겼다. 클래스 설명에 로그인과 로그아웃은 Security 필터가 처리한다고 적었다.
- `JwtCommandService`: `login`을 `issueTokens(memberId, authorities)`로 바꿨다. 인증은 필터가 하므로 서비스는 토큰 발급만 한다.
- `JwtCommandHandler`: `authenticate`와 `AuthenticationManager` 의존을 제거했다.
- 그대로 둔 것: 재발급 로직과 테스트, `JwtTokenResponse`, `JwtRepository`, JSON이 아닌 본문을 400으로 처리하는 `CommonExceptionHandler`

## 테스트

- `LoginTest`: 필터 기준으로 되돌렸다. 앞서 둘로 나눴던 400 테스트(필드 목록 400, JSON이 아닌 본문 400)를 하나로 합쳤다. JSON이 아닌 본문, 빈 객체, 이메일만 있는 본문, 비밀번호가 공백인 본문, 빈 본문이 모두 data 없는 400이고 Redis에 Refresh Token이 남지 않는지 확인한다. 이 때문에 전체 테스트 수가 76개에서 75개가 되었다.
- 나머지 테스트(`ReissueTest`, `JwtRepositoryTest`, `SecurityConfigTest` 등)는 변경 없이 통과했다.

## OpenAPI

- `choimory-dev-member-api-jwt-v1.json`의 로그인 400 예시를 JSON이 아닌 본문, 필드 누락, 비밀번호 공백 모두 data 없는 `BAD_REQUEST` 응답으로 되돌렸다. 400 응답 스키마도 `CommonResponseError`로 바꿨다.
- 로그인 설명에 "로그인은 Spring Security 필터가 처리한다"를 적고, `LoginRequest`의 `minLength`를 뺐다. 문서 상단 설명에도 로그인은 필터가, 재발급은 컨트롤러가 처리한다고 적었다.
- 재발급 부분은 그대로이다. JSON 파싱과 `$ref` 12개 해석을 파이썬으로 확인했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 75개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 복구한 세 파일이 `git`에서 삭제로 스테이징되어 있어 스테이징만 되돌렸다. 이제 수정된 파일로 보인다.
- 로컬 실행 환경변수는 그대로이다: `jwt-secret`(32바이트 이상), `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`

## 남은 사항

- 4단계: 로그아웃(`logout()` 설정과 `LogoutHandler`가 `JwtCommandService.logout`을 호출)과 Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO). 요청 방식(`Authorization: Bearer` + 본문 `{refreshToken}`)은 시작 전에 확인한다.
- OpenAPI에 로그아웃 API 추가
- plan3.md의 "jwt 도메인 전환 결정" 섹션과 task3.md의 "auth 도메인을 jwt 도메인으로 전환" 섹션은 이 섹션으로 대체된 내용이 있다. plan2.md 구조도의 `auth` 경로도 정리할지 결정이 필요하다.
- README에는 반영하지 않았다.

---

# 회원가입 API 명세서 400 예시 추가

> api 문서도 반영했나

## 답변

`jwt` 문서(`choimory-dev-member-api-jwt-v1.json`)는 현재 코드와 일치했지만, `member` 문서(`choimory-dev-member-api-member-v1.json`)의 회원가입 API에는 JSON이 아닌 요청 본문의 400 예시가 빠져 있어 추가했다. 규칙(`api-docs.md`)은 기존 API의 변경을 OpenAPI 파일에도 반영하도록 한다.

## 현재 반영 상태

- `jwt` 문서: 로그인은 200, 400(JSON이 아닌 본문, 필드 누락, 비밀번호 공백, 모두 data 없음), 401, 500, 재발급은 200, 400(필드 누락, JSON이 아닌 본문), 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500이 작성되어 있다.
- `member` 문서: 이번 작업에서 `CommonExceptionHandler`에 `HttpMessageNotReadableException` → 400 처리를 추가했는데, 이전에 500이던 회원가입 API의 JSON이 아닌 본문 응답이 400으로 바뀐 것이 문서에 없었다.

## 추가한 내용

- 회원가입 400 응답에 예시 "JSON이 아닌 본문"을 추가했다. 값은 `{"code":400,"name":"BAD_REQUEST","message":"Bad Request","data":null}`이다.
- 400 응답 설명을 검증 실패와 본문을 읽을 수 없는 경우를 함께 다루도록 고쳤다. 본문 내용은 응답에 담기지 않는다는 점도 적었다.
- API 설명 끝에 "요청 본문이 JSON이 아니면 400이며 data가 없습니다."를 덧붙였다.
- 응답 스키마 `CommonResponseValidateResults`의 `data`를 배열 또는 null로 바꾸고 설명을 맞췄다. `jwt` 문서와 같은 방식이다.

## 검증

- JSON 파싱과 `$ref` 8개 해석을 파이썬으로 확인했다. 끊어진 참조는 없다.
- 400 응답 예시는 "검증 실패", "JSON이 아닌 본문" 두 개가 되었다.
- Apidog 가져오기는 직접 확인하지 못했다.

---

# 로그인 주소를 /v1/login으로 변경

> 로그인 요청주소는 /v1/login으로 하고 싶은데
>
> 1. ㅇㅇ
> 2. 재발급은 유지

## 답변

로그인 주소를 `/v1/jwt/login`에서 `/v1/login`으로 바꿨다. 로그인이 필터라서 컨트롤러 경로 규칙(`/v1/{domain}`)과 충돌하지 않고, 경로는 `LoginFilter`의 상수 한 곳에서 정한다.

## 결정

- 로그인은 `/v1/login`이다.
- 로그아웃도 로그인과 짝이므로 4단계에서 `/v1/logout`으로 만든다.
- 재발급은 `/v1/jwt/reissue`를 유지한다. `jwt` 도메인의 일반 API라서 컨트롤러 경로 규칙을 그대로 따른다.

## 변경 내용

- `LoginFilter.LOGIN_PATH`를 `/v1/login`으로 바꾸고 필터 설명(Javadoc)도 고쳤다.
- `SecurityConfig`: `permitAll` 목록에서 문자열 `"/v1/jwt/login"` 대신 `LoginFilter.LOGIN_PATH` 상수를 쓰도록 했다. 경로를 한 곳에서만 정하기 위해서이고, 필터 연결부 주석도 새 경로로 고쳤다.
- 테스트: `LoginTest`(`POST`와 `GET` 확인 포함), `ReissueTest`의 로그인 호출 경로를 바꿨다.
- OpenAPI(`choimory-dev-member-api-jwt-v1.json`): 로그인 경로 키를 `/v1/login`으로 바꾸고, 상단 설명에 로그인(`/v1/login`)은 Security 필터가, 재발급(`/v1/jwt/reissue`)은 컨트롤러가 처리한다고 적었다. 파일의 경로는 `/v1/login`, `/v1/jwt/reissue` 두 개이다.
- 코드와 문서(`docs/api`)에서 `/v1/jwt/login`은 남아 있지 않다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 75개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 남은 사항

- 4단계: 로그아웃을 `/v1/logout`으로 구현(`logout()` 설정과 `LogoutHandler`가 `JwtCommandService.logout` 호출)하고 Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO)를 함께 구현한다. 요청 방식(`Authorization: Bearer` + 본문 `{refreshToken}`)은 시작 전에 확인한다.
- OpenAPI에 로그아웃 API 추가
- 기록 문서의 옛 경로 `/v1/jwt/login`(plan2.md, plan3.md, 앞쪽 task3.md 섹션)은 히스토리라서 수정하지 않았다. 이 섹션이 정정 역할을 한다.
- README에는 반영하지 않았다. 마무리 단계에서 로그인·로그아웃은 필터라서 컨트롤러 경로 규칙(`/v1/{domain}`)의 예외라는 점을 적어야 한다.

---

# 재발급을 Member 컨트롤러로 이동

> jwt 재발급도 Membercontroller쪽으로 옮기고, jwt 핸들러~리포지토리만 남기면 안돼? memberservice에서 jwt handler, repository 호출하는 식으로
>
> 토큰발급(로그인)은 로그인 필터에서 jwt handler 호출해서 발급하고, 토큰 재발급은 membercontroller에서 jwt handler 호출해서 재발급하는식으로 가자
>
> 네 그 구조로 코드 수정 시작해줘

## 답변

토큰 재발급을 `jwt` 도메인의 컨트롤러에서 `member` 도메인의 컨트롤러로 옮기고, `jwt` 도메인에는 핸들러와 리포지토리만 남겼다. 논의는 plan3.md의 "재발급을 Member 컨트롤러로 옮기는 안"과 "토큰 발급과 재발급 호출 방식 결정"에 있다. 앞의 "auth 도메인을 jwt 도메인으로 전환" 섹션에 적힌 `JwtCommandController`, `JwtCommandService`와 `/v1/jwt/reissue`는 이 섹션으로 대체된다.

## 결정 내용

- 토큰 발급(로그인)은 로그인 필터가 `JwtCommandHandler`를 호출해서 발급한다.
- 토큰 재발급은 `MemberCommandController`에서 `MemberCommandService`를 거쳐 `JwtCommandHandler`를 호출해서 재발급한다.
- 이유: 재발급 중 회원 상태와 권한 조회가 `member` 도메인 안에서 끝나고 `jwt`는 토큰 처리만 하는 하위 모듈이 된다. 의존 방향이 `jwt` → `member`에서 `member` → `jwt`로 바뀐다.
- 서비스는 `JwtRepository`를 직접 호출하지 않고 `JwtCommandHandler`만 호출한다. 컨벤션(`Service → Handler → Repository`)을 따른다.
- 명시적인 답이 없던 재발급 URL, 요청·응답 객체 이름은 제안한 대로 `/v1/member/reissue`, `MemberReissueRequest`, `MemberTokenResponse`로 했다.

## 반영된 구조

| 기능 | 처리 |
|---|---|
| 로그인 `POST /v1/login` | `LoginFilter` → `LoginSuccessHandler` → `JwtCommandHandler.issueTokens` |
| 재발급 `POST /v1/member/reissue` | `MemberCommandController` → `MemberCommandService.reissue` → `MemberCommandHandler.findById`, `JwtCommandHandler` |
| 로그아웃 (4단계) | 로그아웃 필터와 핸들러가 `JwtCommandHandler`를 호출할 예정 |

`jwt` 패키지에는 `JwtProvider`, `JwtProperties`, `TokenType`, `IssuedToken(s)`, `JwtClaims`와 `v1/command/{handler,repository}`만 남았다.

## 코드 변경

- `MemberCommandController`: `POST /reissue`를 추가했다. `MemberCommandController`의 기본 경로가 `/v1/member`라서 재발급 URL은 `/v1/jwt/reissue`에서 `/v1/member/reissue`가 되었다.
- `MemberCommandService`: `JwtCommandHandler` 의존을 추가하고 `reissue`를 구현했다. 순서는 Refresh Token 검증과 사용 처리, 회원 상태와 현재 권한 확인(같은 도메인의 `MemberCommandHandler`), 새 토큰 발급이다. 로직은 `JwtCommandService.reissue`와 같다.
- `LoginSuccessHandler`: `JwtCommandService` 대신 `JwtCommandHandler.issueTokens`를 직접 호출하고 `MemberTokenResponse`로 변환한다. 로그인 필터는 컨트롤러와 서비스 계층 밖이라 서비스를 거치지 않는 예외이고, 이 사유를 클래스 설명에 적었다.
- `SecurityConfig`: `permitAll`의 재발급 경로를 `/v1/member/reissue`로 바꾸고, 클래스 설명을 정정했다.
- 삭제: `JwtCommandController`, `JwtCommandService`
- 객체 이동과 이름 변경: `ReissueRequest` → `member/v1/command/payload/request/MemberReissueRequest`, `JwtTokenResponse` → `member/v1/command/payload/response/MemberTokenResponse`. 로그인 응답과 재발급 응답은 같은 객체를 쓴다.

## 테스트

- `ReissueTest` → `member/v1/command/MemberReissueTest`: 클래스명과 경로(`/v1/member/reissue`)만 바꿨다. rotation, 재사용 거부, 권한 변경 반영, 정지·탈퇴 회원 거부, 만료·잘못된 토큰, 400, 인증 없이 호출 가능 등 테스트 내용은 그대로이다.
- `LoginTest` → `security/LoginTest`: 로그인이 `security` 필터라서 패키지를 옮겼다.
- 테스트 수는 그대로 75개이다.

## OpenAPI

- `choimory-dev-member-api-member-v1.json`: `/v1/member/reissue`와 스키마(`MemberReissueRequest`, `MemberTokenResponse`, `CommonResponseMemberTokenResponse`)를 옮겨 왔다. 경로는 `/v1/member/join`, `/v1/member/reissue` 두 개이고, 상단 설명에 회원가입과 토큰 재발급을 다룬다고 적었다.
- `choimory-dev-member-api-jwt-v1.json`: 로그인(`/v1/login`)만 남기고 쓰지 않는 스키마를 정리했다. 상단 설명에 재발급이 회원 API 명세서에 있다는 점을 적었다.
- 두 문서 모두 JSON 파싱과 `$ref` 해석을 확인했고 끊어진 참조와 쓰지 않는 스키마는 없다. Apidog 가져오기는 확인하지 못했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 75개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 참고

- 클라이언트나 Apidog에서 이전 재발급 주소(`/v1/jwt/reissue`)를 쓰고 있다면 `/v1/member/reissue`로 바꿔야 한다.
- 앞의 "Redis 설정과 로그인 (3단계)", "auth 도메인을 jwt 도메인으로 전환", "로그인 필터 복구" 섹션과 plan2.md 구조도의 옛 구조 설명은 히스토리라서 수정하지 않았다. 이 섹션이 정정 역할을 한다.
- 로컬 실행 환경변수는 그대로이다: `jwt-secret`(32바이트 이상), `redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`

## 남은 사항

- 4단계: 로그아웃을 `/v1/logout`으로 구현(`logout()` 설정과 핸들러가 `JwtCommandHandler`를 호출)하고 Access Token 블랙리스트(`JwtAuthenticationFilter`의 TODO)를 함께 구현한다. 요청 방식(`Authorization: Bearer` + 본문 `{refreshToken}`)은 시작 전에 확인한다.
- OpenAPI에 로그아웃 API 추가
- README에는 반영하지 않았다. 마무리 단계에서 `jwt` 도메인 구성(핸들러·리포지토리 중심), 로그인·로그아웃이 필터라서 컨트롤러 경로 규칙의 예외라는 점, 토큰 관련 URL을 적어야 한다.
