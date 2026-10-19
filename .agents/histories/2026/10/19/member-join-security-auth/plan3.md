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
