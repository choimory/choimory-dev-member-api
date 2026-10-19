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
