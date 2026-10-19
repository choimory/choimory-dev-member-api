# 목차

- [개요](#개요)
- [Security 설정이랑 JWT 설계안 제시](#security-설정이랑-jwt-설계안-제시)
  - [답변](#답변)
  - [전체 구조](#전체-구조)
  - [Security 설정](#security-설정)
  - [JWT](#jwt)
  - [API 요약](#api-요약)
  - [Redis와 설정](#redis와-설정)
  - [작업 순서](#작업-순서)
  - [정해야 할 것](#정해야-할-것)
- [로컬에서 apidog으로 쏴봤는데 401뜨는거 정상?](#로컬에서-apidog으로-쏴봤는데-401뜨는거-정상)
  - [답변](#답변-1)
  - [확인해 볼 수 있는 것](#확인해-볼-수-있는-것)
  - [해결 방법](#해결-방법)
- [그럼 우리 계획대로 plan2의 spring security, jwt 설계 들어가면 되겠네](#그럼-우리-계획대로-plan2의-spring-security-jwt-설계-들어가면-되겠네)
  - [답변](#답변-2)
  - [남은 결정 7가지](#남은-결정-7가지)
  - [진행 방식](#진행-방식)
- [추천안대로 진행하면 어떻게 진행될지 항목별 소개](#추천안대로-진행하면-어떻게-진행될지-항목별-소개)
  - [답변](#답변-3)
  - [추천안 전체의 큰 그림](#추천안-전체의-큰-그림)
  - [Refresh Token을 응답 본문으로 전달](#refresh-token을-응답-본문으로-전달)
  - [Access Token 블랙리스트 사용](#access-token-블랙리스트-사용)
  - [다중 로그인 허용](#다중-로그인-허용)
  - [jjwt 라이브러리](#jjwt-라이브러리)
  - [로그아웃은 logout 설정 방식](#로그아웃은-logout-설정-방식)
  - [auth 도메인과 ACTIVATE만 로그인](#auth-도메인과-activate만-로그인)
  - [만료 시간 30분과 14일](#만료-시간-30분과-14일)
  - [결과적으로 달라지는 점](#결과적으로-달라지는-점)
- [SecurityConfig 위치와 handler 3개의 역할](#securityconfig-위치와-handler-3개의-역할)
  - [답변](#답변-4)
  - [SecurityConfig 위치 변경](#securityconfig-위치-변경)
  - [handler 3개의 역할](#handler-3개의-역할)
  - [JwtProperties 위치](#jwtproperties-위치)
  - [패키지 위치 추가 변경](#패키지-위치-추가-변경)
- [jwt key에 accesskey는 필요없어?](#jwt-key에-accesskey는-필요없어)
  - [답변](#답변-5)
  - [지금 구조](#지금-구조)
  - [키를 따로 두면 달라지는 것](#키를-따로-두면-달라지는-것)
  - [판단](#판단)
  - [정해야 할 것](#정해야-할-것-1)
- [설정에 jwt-secret 이것만 있으면 돼?](#설정에-jwt-secret-이것만-있으면-돼)
  - [답변](#답변-6)
  - [이유](#이유)
  - [토큰은 암호화되지 않는다](#토큰은-암호화되지-않는다)
  - [그 밖에 앞으로 필요한 설정값](#그-밖에-앞으로-필요한-설정값)
  - [나중에 키가 늘어날 수 있는 경우](#나중에-키가-늘어날-수-있는-경우)
- [access token이랑 secret token을 별도 Key로 관리하는 안](#access-token이랑-secret-token을-별도-key로-관리하는-안)
  - [답변](#답변-7)
  - [키를 따로 관리하면 생기는 이점](#키를-따로-관리하면-생기는-이점)
  - [우리 구조에서 이점이 작은 이유](#우리-구조에서-이점이-작은-이유)
  - [바꾸게 되면 달라지는 곳](#바꾸게-되면-달라지는-곳)
  - [남은 결정](#남은-결정)

---

# 개요

- plan1.md, task1.md에서 회원가입 API를 구현했다. 이어서 Spring Security 기본 구조와 JWT를 설계한다.
- 로그인, 로그아웃은 Spring Security 필터로 작업하고, 토큰 재발급 API도 작업한다. Refresh Token 저장소로는 Redis를 사용한다.
- 작업 순서는 Security와 JWT, 토큰 발급, 재발급 순이다.
- 참고: plan1.md, task1.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# Security 설정이랑 JWT 설계안 제시

> Security 설정이랑 JWT 설계안 제시해줘

## 답변

`SecurityConfig`, JWT, Redis 설계안이다. 확인한 현재 상태는 다음과 같고, 이 시점에는 파일을 만들지 않았다.

- Spring Boot 3.5.4(Spring Security 6.5)이고, JWT 라이브러리는 없다.
- `member-api`에 Redis 의존성만 있고 설정은 없다.
- yml은 `application-datasource.yml`처럼 프로필별 파일로 나뉘어 있고 연결 정보는 환경변수로 받는다.

## 전체 구조

```
dev.choimory.member.api
├── security
│   ├── filter      JwtAuthenticationFilter, LoginFilter
│   ├── handler     AuthenticationEntryPoint, AccessDeniedHandler, 로그인 성공/실패 핸들러, 로그아웃 핸들러
│   └── user        MemberUserDetailsService, AuthenticatedMember(UserDetails)
├── jwt             JwtProvider (생성·검증·파싱), JwtProperties
├── config          PasswordEncoderConfig (그대로), SecurityConfig
└── auth/v1/command   (신규 도메인) controller(reissue) → service → handler → repository(Redis)
```

- `PasswordEncoderConfig`는 별도로 유지한다. `SecurityConfig`에 합치면 `UserDetailsService`에서 `MemberCommandHandler`, `PasswordEncoder`로 이어지는 순환 의존이 생긴다.
- 로그인·로그아웃은 필터에서, 재발급은 일반 API로 처리하므로, 토큰 발급·재발급·폐기 로직은 새 `auth` 도메인의 Handler 하나(`AuthCommandHandler`)에 모은다. 로그인 성공 핸들러도 이 Handler를 호출한다. README에 도메인이 4개라고 적혀 있어서 `auth` 도메인을 추가하면 README도 고쳐야 한다.
- Redis 접근은 컨벤션의 Repository 역할로 `AuthTokenRepository`(`StringRedisTemplate`)를 둔다.

## Security 설정

- 기본: stateless 세션, CSRF·formLogin·httpBasic 비활성화. CORS는 클라이언트가 정해지면 추가한다.
- 허용 주소: `POST /v1/member/join`, `POST /v1/auth/login`, `POST /v1/auth/reissue`는 `permitAll`이고, 나머지는 인증이 필요하다.
- 필터 순서: `JwtAuthenticationFilter`가 `UsernamePasswordAuthenticationFilter` 앞에, `LoginFilter`가 그 자리를 대신한다.
- 로그인 필터: `UsernamePasswordAuthenticationFilter`를 상속한 `LoginFilter`가 `POST /v1/auth/login`의 `{email, password}` JSON을 받아 `AuthenticationManager`로 인증한다. 성공 시 토큰을 발급해 응답하고, 실패 시 401이다. 로그인 요청에는 비밀번호 규칙 검증을 하지 않고 비어 있는지만 본다. 규칙이 노출되지 않게 하기 위해서이다.
- 로그아웃 필터: 직접 필터 클래스를 만들지 않고 Spring Security의 `logout()` 설정에 `LogoutHandler`(토큰 폐기)와 `LogoutSuccessHandler`(응답)를 붙이는 방식을 추천한다. 내부적으로 `LogoutFilter`가 동작하므로 "필터로 작업"한다는 의도에 맞다.
- 인증 정보: `MemberUserDetailsService`가 이메일로 회원을 조회해 `AuthenticatedMember`(id, email, 비밀번호, 권한 `ROLE_{AuthorityCode}`)를 만든다. 조회는 `MemberCommandHandler`에 `findByEmail`을 추가해 읽는다. `MemberQueryHandler`는 Elasticsearch용이라 쓸 수 없다. `UserDetailsService`는 Security가 호출하므로 Service 역할로 보고 Handler를 직접 부른다.
- 필터 단계 예외 처리: 필터에서 난 예외는 `@RestControllerAdvice`에 닿지 않는다. 그래서 `AuthenticationEntryPoint`(401), `AccessDeniedHandler`(403), 로그인 실패 핸들러가 직접 `CommonResponse` JSON을 쓴다. 응답 작성은 공통 컴포넌트 하나로 모은다.
- 로그인 가능 상태: `ACTIVATE` 회원만 로그인할 수 있게 하는 안이다. `REGISTER`는 이후 이메일 인증용이다. 상태별로 응답을 달리 하면 계정 존재·상태가 노출되므로 일단 모두 같은 401로 처리하는 안을 제안한다.

## JWT

- 라이브러리: `jjwt` 0.12.x. 대안은 `oauth2-resource-server`(Nimbus)로, 베어러 필터가 이미 제공되지만 커스텀 필터를 직접 작성하려는 방향과는 맞지 않아 jjwt를 추천한다.
- 서명: HS256. 키는 256비트 이상이며 환경변수 `jwt-secret`으로 받는다. `JwtProperties`(`secret`, `issuer`, `access-expiration`, `refresh-expiration`)로 묶는다.
- Access Token: 만료 30분. 클레임은 `sub`=회원 id, `jti`, `type`="access", 권한 코드 목록이다. `JwtAuthenticationFilter`는 DB 조회 없이 토큰만으로 인증을 만든다.
- Refresh Token: 만료 14일. 클레임은 `sub`, `jti`, `type`="refresh"이다. Redis에 `refresh:{memberId}:{jti}` 키로 저장하고 TTL을 만료와 맞춘다. 기기별로 구분되는 다중 로그인을 허용하는 안이다.
- 재발급: `POST /v1/auth/reissue`에서 Refresh Token을 검증하고 Redis에 있는지 확인한 뒤, 기존 키를 지우고 새 쌍을 발급하는 rotation 방식이다. Redis에 없는 Refresh Token은 이미 쓰였거나 폐기된 것으로 보고 401로 거부한다.
- 로그아웃: 보낸 Refresh Token의 Redis 키를 삭제하고, Access Token의 `jti`를 남은 만료 시간만큼 블랙리스트(`blacklist:{jti}`)에 올린다. 이 경우 `JwtAuthenticationFilter`가 매 요청마다 Redis를 한 번 조회하는 비용이 생긴다.
- 토큰 오류 구분: 만료는 `TOKEN_EXPIRED`, 그 외 잘못된 토큰은 `INVALID_TOKEN` 같은 코드명으로 구분해 클라이언트가 재발급할지 판단하게 한다. 모두 `CommonException`으로 처리하는 방침에 맞춰 non-critical(401)로 분류한다.

## API 요약

| 주소 | 입력 | 응답 |
|---|---|---|
| `POST /v1/auth/login` | `{email, password}` | `{accessToken, refreshToken, ...}` |
| `POST /v1/auth/reissue` | `{refreshToken}` | 새 `{accessToken, refreshToken, ...}` |
| `POST /v1/auth/logout` | `Authorization: Bearer`, `{refreshToken}` | 200 |

## Redis와 설정

- `application-redis.yml`을 `datasource`처럼 프로필로 분리하고 `redis-host`, `redis-port`, `redis-password` 환경변수로 받는 안이다.
- 테스트는 Testcontainers Redis(`redis:7`)를 `TestcontainersConfiguration`에 추가해 `@ServiceConnection`으로 연결한다.
- 지금 `MemberJoinTest`는 Security 필터를 끈 상태인데, Security 설정이 생기면 필터를 켠 채로 가입이 허용되는지(`/v1/member/join`이 열리는지)까지 같이 검증하도록 바꾼다.
- `@EnableMethodSecurity`로 `hasRole('ADMIN')` 같은 역할 제한을 쓸 수 있게 하되, 메뉴·권한(permission) 기반 동적 인가는 이번 범위에서 제외한다.

## 작업 순서

말씀한 순서 기준이다.

1. Security 기본 설정: `SecurityConfig`, 응답 작성 컴포넌트, EntryPoint·AccessDeniedHandler, `/v1/member/join` 허용
2. JWT: `JwtProperties`, `JwtProvider`, `JwtAuthenticationFilter`, `AuthenticatedMember`
3. Redis 설정과 로그인(토큰 발급): `LoginFilter`, `MemberUserDetailsService`, `AuthCommandHandler`, `AuthTokenRepository`
4. 로그아웃: `logout()` 설정과 핸들러, 블랙리스트
5. 토큰 재발급 API
6. 각 단계마다 테스트 후 확인, 끝나면 README 반영

## 정해야 할 것

각 항목에 추천안을 같이 적었다.

1. Refresh Token 전달 방식: 응답 본문(추천, 클라이언트가 아직 정해지지 않아 변경이 쉬움) 또는 HttpOnly 쿠키
2. Access Token 블랙리스트: 로그아웃 시 사용한다(추천). 쓰지 않으면 로그아웃해도 Access Token이 최대 30분 유효하다.
3. 다중 로그인 허용(추천)과 회원당 1개만 허용 중 무엇으로 할지
4. JWT 라이브러리: jjwt(추천)로 갈지
5. 로그아웃: Spring `logout()` 설정 방식(추천)으로 갈지, 직접 필터 클래스를 만들지
6. `auth` 도메인 추가와 로그인 가능 상태(`ACTIVATE`만, 응답 구분 없음)에 동의하는지
7. 만료 시간 30분/14일이 괜찮은지

---

# 로컬에서 apidog으로 쏴봤는데 401뜨는거 정상?

> 로컬에서 apidog으로 쏴봤는데 401뜨는거 정상?

## 답변

정상이다. 지금은 `SecurityConfig`가 없어서 Spring Boot의 기본 보안 설정이 모든 요청에 인증을 요구하기 때문이다. 회원가입 API가 구현된 것과 별개로, 가입 주소를 열어 주는 설정이 아직 없다. 구현 직후 설명한 "Security 때문에 실제 호출은 막힌다"가 이 상황이다. 통합 테스트는 `addFilters = false`로 필터를 꺼서 통과했다.

## 확인해 볼 수 있는 것

- 응답 헤더에 `WWW-Authenticate: Basic ...`이 있고 본문이 비어 있으면 기본 보안 설정이 막은 것이다.
- 이 응답은 필터 단계에서 만들어져서 `CommonExceptionHandler`를 거치지 않는다. 그래서 `CommonResponse` JSON 형태가 아니라 본문이 비어 있다.
- 기본 설정에서는 CSRF도 켜져 있어서 인증 정보 없는 POST는 이론상 403이 날 수도 있다. 401이 나온 이유까지는 직접 확인하지 못했다. 둘 다 같은 원인(기본 보안 설정)이라 해결 방법은 같다.

## 해결 방법

작업 순서 1단계(Security 기본 설정)에서 `SecurityConfig`를 만들고 `POST /v1/member/join`을 `permitAll`로 열고, 401·403 응답을 `CommonResponse` JSON으로 내려주는 EntryPoint·AccessDeniedHandler를 넣으면 해결된다. 가입 API를 바로 쓰려면 최소 버전(CSRF 비활성화, stateless, `/v1/member/join` 허용, 나머지 인증 필요)만 먼저 추가하는 방법도 있다. 또 가입 호출에는 DB 연결(`rdb-*` 환경변수)과 `V3` 권한 데이터 적용(Flyway)이 필요하다.

---

# 그럼 우리 계획대로 plan2의 spring security, jwt 설계 들어가면 되겠네

> 그럼 우리 계획대로 plan2의 spring security, jwt 설계 들어가면 되겠네

## 답변

`plan2.md`의 설계로 진행하면 된다. 구현에 들어가기 전에 아직 답을 받지 못한 7가지가 남아 있다. 각 항목마다 추천안을 적었고, "추천안대로"라고만 해도 된다.

## 남은 결정 7가지

| # | 항목 | 추천안 | 다른 선택지 |
|---|---|---|---|
| 1 | Refresh Token 전달 | 응답 본문(클라이언트가 정해지지 않아 변경이 쉬움) | HttpOnly 쿠키 |
| 2 | Access Token 블랙리스트 | 로그아웃 시 사용 (매 요청 Redis 1회 조회 비용) | 미사용 (로그아웃 후 최대 30분 유효) |
| 3 | 로그인 허용 범위 | 다중 로그인 허용 (`refresh:{memberId}:{jti}`) | 회원당 1개만 허용 |
| 4 | JWT 라이브러리 | jjwt 0.12.x | oauth2-resource-server(Nimbus) |
| 5 | 로그아웃 | Spring `logout()` 설정 + 핸들러 | 직접 필터 클래스 작성 |
| 6 | `auth` 도메인 추가, 로그인 가능 상태 | 추가 + `ACTIVATE`만 로그인, 실패 응답은 모두 같은 401 | `auth` 없이 member 안에 둠 / 상태별 응답 구분 |
| 7 | 만료 시간 | Access 30분, Refresh 14일 | 다른 값 |

## 진행 방식

- 7가지가 정해지면 `plan2.md`에 결정 사항을 기록하고, 작업 순서의 1단계(Security 기본 설정)부터 시작 여부를 확인받고 진행한다.
- 1단계가 끝나면 Apidog에서 가입 API를 다시 호출해 볼 수 있다. 1단계에 `POST /v1/member/join` 허용과 401·403 JSON 응답이 들어 있기 때문이다.
- `calendar.md`는 이미 TASK 상태라 바꾸지 않는다.

---

# 추천안대로 진행하면 어떻게 진행될지 항목별 소개

> 추천안대로 하고 싶긴한데 추천안대로 진행하면 어떻게 진행될지가 좀 감이 안잡히네. 지금 내용 plan2에 추가한다음에 항목별로 자세히 소개해줘

## 답변

남은 결정 7가지를 추천안으로 진행했을 때 어떻게 동작하는지 항목별로 소개한다.

## 추천안 전체의 큰 그림

```
가입 → (permitAll) POST /v1/member/join → 회원 저장
로그인 → POST /v1/auth/login → Access + Refresh 발급 (Redis에 Refresh 저장)
API 호출 → Authorization: Bearer {access} → JwtAuthenticationFilter가 검증
Access 만료 → POST /v1/auth/reissue {refresh} → 새 Access + 새 Refresh
로그아웃 → POST /v1/auth/logout → Refresh 삭제 + Access 블랙리스트 등록
```

## Refresh Token을 응답 본문으로 전달

- 로그인 응답: `{"code":200, ..., "data":{"accessToken":"eyJ...", "refreshToken":"eyJ...", "accessExpiresIn":1800, "refreshExpiresIn":1209600}}`
- 클라이언트가 두 토큰을 직접 저장하고, 재발급·로그아웃 요청 본문에 Refresh Token을 담아 보낸다.
- 장점: 모바일·웹·Apidog 어디서든 똑같이 쓰고, 쿠키 설정(SameSite, Secure, CORS)을 신경 쓰지 않는다.
- 단점: 브라우저에서 JS가 토큰을 읽을 수 있어서 XSS에 노출된다. 나중에 웹 클라이언트가 정해지면 쿠키 방식으로 바꿀 수 있고, 바꾸는 범위는 로그인·재발급 응답과 요청 부분이다.

## Access Token 블랙리스트 사용

- Access Token은 서명만 맞으면 만료 전까지 유효해서, 로그아웃해도 이미 발급된 토큰은 살아 있다. 이를 막으려는 장치이다.
- 로그아웃 시: 토큰의 `jti`를 `blacklist:{jti}` 키로 Redis에 넣고 TTL은 토큰의 남은 만료 시간으로 둔다. TTL이 지나면 자동으로 사라진다.
- 매 요청마다 `JwtAuthenticationFilter`가 서명·만료 검증 뒤에 Redis에서 `blacklist:{jti}`를 한 번 조회한다. 있으면 401이다.
- 비용은 요청당 Redis 조회 1회이다. 안 쓰면 이 조회가 없어지는 대신 로그아웃 후에도 최대 30분은 같은 토큰이 통한다.

## 다중 로그인 허용

- 로그인할 때마다 새 `jti`로 Refresh Token을 만들고 `refresh:{memberId}:{jti}` 키로 저장한다.
- 한 회원이 휴대폰과 PC에서 동시에 로그인해도 서로를 로그아웃시키지 않는다. 각 기기가 자기 Refresh Token을 가진다.
- 로그아웃은 보낸 Refresh Token의 키 하나만 지운다. "모든 기기에서 로그아웃"은 `refresh:{memberId}:*`를 지우는 기능으로 나중에 추가할 수 있다.
- 회원당 1개만 허용하면 새 로그인이 이전 로그인을 끊는다. 구현은 더 단순하다(키가 `refresh:{memberId}` 하나).

## jjwt 라이브러리

- 의존성 `io.jsonwebtoken:jjwt-api`, `jjwt-impl`, `jjwt-jackson`(0.12.x)을 추가한다.
- `JwtProvider`에서 `Jwts.builder()`로 토큰을 만들고 `Jwts.parser()`로 검증·파싱한다. 만료는 `ExpiredJwtException`, 그 외 오류는 `JwtException`으로 오기 때문에 `TOKEN_EXPIRED`와 `INVALID_TOKEN` 구분이 쉽다.
- Nimbus 방식은 베어러 필터와 디코더가 제공되지만, 인증 객체 구성과 오류 응답을 Spring의 정해진 틀에 맞춰야 해서 직접 만드는 필터와는 방향이 다르다.

## 로그아웃은 logout 설정 방식

- `SecurityConfig`에 `http.logout(l -> l.logoutUrl("/v1/auth/logout").addLogoutHandler(...).logoutSuccessHandler(...))`를 설정한다.
- 핸들러가 Refresh Token 삭제와 Access Token 블랙리스트 등록을 하고, 성공 핸들러가 `CommonResponse` 200을 쓴다.
- 내부적으로는 Spring이 제공하는 `LogoutFilter`가 동작하므로 "필터로 처리"하는 방식은 같고, 직접 만드는 코드가 줄어든다. 로그인은 커스텀 `LoginFilter`, 로그아웃은 Spring 기본 필터를 조합하는 모양이다.

## auth 도메인과 ACTIVATE만 로그인

- `dev.choimory.member.api.auth.v1.command` 아래에 `controller`(재발급), `service`, `handler`(`AuthCommandHandler`), `repository`(`AuthTokenRepository`, Redis)를 둔다. 토큰 발급·재발급·폐기 로직이 이곳에 모이고, 로그인·로그아웃 필터 쪽 핸들러는 이 Handler를 호출한다.
- 로그인 가능 상태는 `ACTIVATE`만이다. 정지·비활성·가입 대기 회원이나 없는 이메일, 틀린 비밀번호가 모두 같은 401 응답을 받는다. 어떤 이메일이 가입돼 있는지 외부에서 알아내기 어렵다.
- 단점은 정지된 회원이 "정지됐다"는 안내를 못 받는다는 점이다. 필요해지면 상태별 응답을 나중에 구분할 수 있다.

## 만료 시간 30분과 14일

- Access는 짧게 둬서 탈취돼도 피해 시간이 제한되고, Refresh는 길게 둬서 사용자가 자주 다시 로그인하지 않게 한다.
- 값은 `application` 설정(`jwt.access-expiration`, `jwt.refresh-expiration`)이라 코드를 고치지 않고 바꿀 수 있다.

## 결과적으로 달라지는 점

- 새 파일은 대략 20개 안팎이다(`SecurityConfig`, `JwtProvider`, 필터 2개, 핸들러 여러 개, `auth` 도메인 클래스, Redis 설정, 테스트).
- `application-redis.yml`과 환경변수 `jwt-secret`, `redis-host` 등이 필요하고, 테스트에는 Redis Testcontainers가 추가된다.
- 기존 `MemberJoinTest`는 필터를 켠 상태로 바뀐다.

---

# SecurityConfig 위치와 handler 3개의 역할

> 일단 security config는 common말고 config로 가야지. 그리고 security의 handler 3개는 하는 역할이 뭐야?

## 답변

`SecurityConfig`를 `config` 패키지로 옮기고 handler 3개의 역할을 설명했다. 이후 `plan2.md`의 구조도와 `task2.md`의 경로도 새 위치로 수정했다.

## SecurityConfig 위치 변경

- `SecurityConfig`를 `member-api/.../api/config/SecurityConfig.java`로 옮겼다. `JpaConfig`, `PasswordEncoderConfig`와 같은 위치이다. 패키지 선언만 바꿨고 `clean build`가 통과했다. 코드에서 옛 경로를 쓰는 곳은 없었다.
- `SecurityConfigTest`는 패키지 이름이 아니라 동작을 검증하므로 그대로 통과한다.

## handler 3개의 역할

핸들러가 필요한 이유: 컨트롤러에서 난 예외는 `CommonExceptionHandler`(`@RestControllerAdvice`)가 JSON으로 바꿔 주지만, Security 필터에서 난 인증·인가 실패는 컨트롤러에 도달하기 전이라 거기에 닿지 않는다. 그대로 두면 본문이 비거나 Spring 기본 형식으로 나간다(Apidog에서 본 빈 401). 그래서 필터 단계의 실패를 `CommonResponse` JSON으로 바꿔 주는 클래스가 필요하다.

| 클래스 | 하는 일 | 호출되는 때 |
|---|---|---|
| `CommonAuthenticationEntryPoint` | 401 응답을 JSON으로 작성 | 인증이 필요한 주소에 인증 없이(토큰 없음, 만료 등) 접근했을 때 |
| `CommonAccessDeniedHandler` | 403 응답을 JSON으로 작성 | 인증은 됐지만 권한이 없을 때(예: `ADMIN` 전용 주소에 `MEMBER`가 접근). 지금은 `hasRole`을 쓰는 주소가 없어서 아직 발생하지 않음 |
| `SecurityResponseWriter` | 위 둘이 공통으로 쓰는 응답 작성 도구: 상태 코드, 헤더(JSON, UTF-8), `CommonResponse` 본문을 씀 | 위 두 핸들러가 호출 |

- 정확히는 Spring Security 인터페이스를 구현한 핸들러는 앞의 둘이고, `SecurityResponseWriter`는 둘이 같은 코드를 중복하지 않게 모은 도구이다.
- 앞으로 로그인 실패 핸들러(3단계), 로그아웃 성공 핸들러(4단계)도 이 도구를 같이 쓴다.
- 지금 `SecurityConfig`의 `exceptionHandling`에 앞의 둘이 등록돼 있다.

## JwtProperties 위치

`JwtProperties`(설정값 객체)는 `config`가 아니라 `jwt` 패키지에 `JwtProvider`와 함께 둔다. 2단계에서 그 위치에 만든다.

## 패키지 위치 추가 변경

- `common/security`를 `security`로 옮겼다. 핸들러는 `member-api/.../api/security/handler`에 있다. `common`에는 `exception`만 남는다.
- `jwt`는 `security` 아래가 아니라 `api` 바로 아래(`dev.choimory.member.api.jwt`)에 둔다. `JwtProvider`와 `JwtProperties`를 이 패키지에 만든다.
- 테스트도 같은 구조로 `test/.../api/security`로 옮겼고, 패키지 선언과 import를 모두 고쳤다. `clean build`가 통과했다.

---

# jwt key에 accesskey는 필요없어?

> jwt key에 accesskey는 필요없어?

## 답변

질문을 Access/Refresh 토큰마다 서명 키를 따로 두는 것으로 이해하고 답했다. 정확한 의미가 다르면 다시 확인하기로 했다.

## 지금 구조

키 하나(`jwt-secret`)로 Access Token과 Refresh Token을 같은 키로 서명하고, 토큰 안의 `type` 클레임(`access`/`refresh`)으로 구분한다. `JwtProvider.parse(token, 기대 종류)`가 종류가 다르면 `INVALID_TOKEN`으로 거부하므로 Refresh Token을 Access 자리에 쓰는 것은 막혀 있다. 이 동작은 `JwtProviderTest`와 `JwtAuthenticationFilterTest`로 확인했다.

## 키를 따로 두면 달라지는 것

| | 키 1개 | 키 2개 (access / refresh) |
|---|---|---|
| 종류 혼용 방지 | `type` 클레임 검사 | 서명 자체가 달라 검증에서 실패 (이중 방어) |
| 키 하나가 유출되면 | 두 종류 모두 위조 가능 | 유출된 종류만 위조 가능 |
| 설정 | `jwt-secret` 1개 | `jwt-access-secret`, `jwt-refresh-secret` 2개 |
| 코드 | 지금 그대로 | `JwtProperties`와 `JwtProvider`에 종류별 키 분기 |

## 판단

키를 따로 둘 필요는 크지 않다.

- 두 키 모두 같은 서버의 같은 환경변수에서 읽기 때문에, 환경이 털리면 두 키가 함께 유출된다. 유출된 쪽만 위조된다는 이점이 실제로는 거의 없다.
- 혼용 방지는 `type` 클레임 검사로 이미 된다.
- Refresh Token은 서명 검증 외에 Redis에 저장된 토큰인지도 확인하므로(3단계) 위조된 Refresh Token은 서버 상태로 한 번 더 걸러진다.
- 키가 둘이면 키 교체(rotation) 때 관리 대상이 늘어난다.

다만 키 유출 시 영향 범위를 줄이고 싶거나 보안 점검에서 요구한다면 따로 두는 것도 어렵지 않다. `JwtProperties`를 `accessSecret`, `refreshSecret`으로 나누고 `JwtProvider`가 종류에 맞는 키를 쓰게 바꾸면 되고, 변경은 `JwtProvider`, 설정 파일, 테스트 정도이다.

## 정해야 할 것

1. 키 하나로 유지할지, 둘로 나눌지 (추천: 하나)
2. "accesskey"가 다른 의미였다면 알려 주기. 예를 들어 클라이언트가 API를 호출할 때 쓰는 키(API key) 같은 것인지.

---

# 설정에 jwt-secret 이것만 있으면 돼?

> 설정에 jwt-secret 이것만 있으면 돼? encrypt 키나 access 키 이런건 필요없고?

## 답변

지금은 `jwt-secret` 하나면 충분하다.

## 이유

- 지금 방식은 서명된 JWT(JWS, HS256)이다. 서명 키 하나로 토큰을 만들고 검증하므로 필요한 키는 이것뿐이다.
- encrypt 키는 토큰 내용을 암호화하는 JWE 방식에서만 필요하다. 우리는 쓰지 않는다.
- access 키라는 별도 개념은 JWT에 없다. Access Token과 Refresh Token을 서명할 키를 나눈다는 뜻이라면 앞에서 설명한 대로 하나로 충분하다.

## 토큰은 암호화되지 않는다

서명은 변조를 막을 뿐, 토큰 내용은 누구나 Base64로 읽을 수 있다. 우리 토큰의 클레임은 회원 ID, 권한 코드, 토큰 종류, jti, 발급자, 발급·만료 시각뿐이라 민감한 정보가 없다. 이메일이나 비밀번호 같은 값은 토큰에 넣지 않는 규칙을 유지하면 암호화는 필요 없다.

## 그 밖에 앞으로 필요한 설정값

JWT 키와 별개인 인프라 설정이다.

- 3단계: Redis 접속 정보(`redis-host`, `redis-port`, `redis-password`)
- 비밀번호 해싱(BCrypt)은 키 없이 솔트를 내장하므로 설정값이 필요 없다.

## 나중에 키가 늘어날 수 있는 경우

다른 서비스가 이 토큰을 검증하게 되면, HS256은 검증하는 쪽도 같은 비밀키를 알아야 하므로 부담이 된다. 그때는 비대칭 서명(RS256: 비공개 키로 서명, 공개 키로 검증)으로 바꾸는 것을 고려한다. 지금은 이 서버만 토큰을 검증하므로 해당 없다.

---

# access token이랑 secret token을 별도 Key로 관리하는 안

> access token이랑 secret token을 별도로 가져가서 Key도 두개로 관리하는건?

## 답변

흔히 쓰는 방식이고 틀린 선택은 아니다. 다만 우리 구조에서는 얻는 이득이 작아서 키 하나를 유지하는 쪽을 약하게 추천한다.

## 키를 따로 관리하면 생기는 이점

- 종류 혼용이 서명 단계에서 막힌다. 지금은 `type` 클레임 검사로 막고 있고, 키가 다르면 검사 이전에 서명 검증에서 실패한다. 방어가 한 겹 늘어난다.
- 키를 독립적으로 교체할 수 있다. Refresh 키만 바꾸면 모든 Refresh Token이 한 번에 무효가 되어 전체 강제 로그아웃이 되고, Access 키만 바꾸면 Access Token만 무효가 되어 클라이언트가 재발급으로 복구한다. 다만 우리는 Refresh Token이 Redis에 저장되므로 Redis 키를 지우는 것으로도 같은 효과를 낼 수 있다.
- 정책이 갈린다. 예를 들어 나중에 Refresh Token 검증을 인증 전용 서버로만 옮기는 경우, 그 서버만 Refresh 키를 가지게 할 수 있다.

## 우리 구조에서 이점이 작은 이유

- 두 키가 같은 서버의 같은 환경변수에서 읽히므로, 환경이 털리면 두 키가 함께 유출된다.
- 종류 혼용은 이미 `type` 클레임과 `JwtProviderTest`, `JwtAuthenticationFilterTest`로 막혀 있다.
- Refresh Token은 서명 검증 뒤에 Redis에 있는 토큰인지도 확인하므로(3단계) 위조된 Refresh Token은 서버 상태로 한 번 더 걸러진다.
- 반대로 비용이 있다. 환경변수와 설정이 2개로 늘고, `JwtProvider`가 종류마다 키를 골라야 하며, 키 교체와 테스트 구성이 복잡해진다.

## 바꾸게 되면 달라지는 곳

- `application-jwt.yml`: `jwt.secret` 대신 `jwt.access-secret`, `jwt.refresh-secret` (환경변수 `jwt-access-secret`, `jwt-refresh-secret`)
- `JwtProperties`, `JwtProvider`: 종류별 키 필드, 발급·검증 시 종류에 맞는 키 사용
- 테스트: `build.gradle`의 시스템 프로퍼티 2개, 기존 `JwtProviderTest`, `JwtAuthenticationFilterTest` 보강. 종류 혼용 테스트는 서명 실패로 바뀐다.
- 변경 범위는 2단계 코드 안이라 크지 않다.

## 남은 결정

1. 키 하나(현재)로 유지할지, 둘로 나눌지. 독립적인 교체나 정책 분리가 필요하면 둘이 맞다.
2. 나눈다면 지금 바로 2단계 코드를 고치고 3단계로 넘어간다.
