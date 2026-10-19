# 목차

- [개요](#개요)
- [4단계 로그아웃 계획](#4단계-로그아웃-계획)
  - [답변](#답변)
  - [목표](#목표)
  - [이전 추천 변경](#이전-추천-변경)
  - [요청과 응답](#요청과-응답)
  - [동작](#동작)
  - [블랙리스트](#블랙리스트)
  - [구성](#구성)
  - [테스트](#테스트)
  - [정해야 할 것](#정해야-할-것)
- [4단계 구현 후 API 문서 점검과 정리 제안](#4단계-구현-후-api-문서-점검과-정리-제안)
  - [답변](#답변-1)
  - [점검 결과](#점검-결과)
  - [로그아웃 헤더 추가 (반영함)](#로그아웃-헤더-추가-반영함)
  - [문서의 jwt 도메인 이름](#문서의-jwt-도메인-이름)
  - [Common 접두어 제거 제안](#common-접두어-제거-제안)
  - [데이터 객체 위치 제안](#데이터-객체-위치-제안)
- [문서 이름 결정과 핸들러 이름, 데이터 객체 위치](#문서-이름-결정과-핸들러-이름-데이터-객체-위치)
  - [답변](#답변-2)
  - [문서 이름 변경 (반영함)](#문서-이름-변경-반영함)
  - [접두어를 모두 빼는 경우](#접두어를-모두-빼는-경우)
  - [payload 아래에 두는 안](#payload-아래에-두는-안)
- [데이터 객체를 member-domain 모듈에 두는 안](#데이터-객체를-member-domain-모듈에-두는-안)
  - [답변](#답변-3)
  - [되는 점](#되는-점)
  - [맞지 않는 점](#맞지-않는-점)
  - [옮기는 쪽이 맞는 경우](#옮기는-쪽이-맞는-경우)
  - [위치별 평가](#위치별-평가)
  - [미정 사항](#미정-사항)

---

# 개요

- plan2.md의 설계 4단계: 로그아웃과 Access Token 블랙리스트를 구현한다.
- 1단계(Security 기본 설정), 2단계(JWT), 3단계(Redis 설정과 로그인)와 이후 구조 변경은 task2.md, task3.md에 기록되어 있다. 현재 구조는 로그인 `POST /v1/login`이 `LoginFilter`, 재발급 `POST /v1/member/reissue`가 `MemberCommandController`, 토큰 처리는 `jwt` 도메인의 `JwtCommandHandler`와 `JwtRepository`가 담당한다.
- 로그아웃도 로그인처럼 Security 필터에서 처리하고 `JwtCommandHandler`를 직접 호출한다. 주소는 로그인과 짝인 `/v1/logout`이다.
- 참고: plan2.md, plan3.md, task3.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# 4단계 로그아웃 계획

> 4단계 로그아웃 계획 말해줘

## 답변

4단계(로그아웃) 계획이다. 이 시점에는 코드를 작성하지 않았다.

## 목표

`POST /v1/logout`으로 로그아웃하면 Refresh Token을 폐기하고, 이미 발급된 Access Token도 만료 전에 쓰지 못하게 블랙리스트에 올린다. 지금은 `JwtAuthenticationFilter`에 `// TODO 4단계`로 남아 있는 부분이다.

## 이전 추천 변경

3단계에서 "Spring `logout()` 설정 + 핸들러"를 추천했는데, 다시 보니 맞지 않다.

- Spring의 `LogoutFilter`는 `JwtAuthenticationFilter`보다 앞에서 동작해서, 로그아웃 핸들러가 호출될 때 인증 정보가 아직 비어 있다.
- 핸들러는 오류를 응답으로 내려주기 어렵다. 예외를 던지면 필터 단계라서 500이 되고, 성공 핸들러와 응답이 겹칠 수 있다.

그래서 `LoginFilter`처럼 직접 만든 `LogoutFilter`(`security/filter`)를 추천한다. `JwtAuthenticationFilter` 뒤에 두고, 오류 응답을 직접 쓸 수 있다. Spring Security의 `org.springframework.security.web.authentication.logout.LogoutFilter`와 이름이 같지만, `logout()` 설정을 사용하지 않아 체인에 Spring의 필터가 없고 우리 필터는 스프링 빈이 아니라 `SecurityConfig`에서 직접 생성하므로 충돌하지 않는다. 클래스 설명(Javadoc)에 Spring의 필터와 다르다는 점을 적고, IDE 자동 import에 주의한다.

## 요청과 응답

- `POST /v1/logout` 본문 `{"refreshToken": "..."}`, 헤더 `Authorization: Bearer {accessToken}`는 선택
- 성공: 200, `data` 없음
- 인증 기준을 Refresh Token으로 두는 안이다. Access Token이 이미 만료된 상태여도 로그아웃할 수 있게 하기 위해서이다. Access Token이 필수면 만료 시 먼저 재발급을 받아야 한다.

## 동작

1. 본문이 JSON이 아니거나 `refreshToken`이 비어 있으면 400(data 없음)
2. Refresh Token을 검증한다(서명, 발급자, 만료, 종류). 만료는 `TOKEN_EXPIRED`, 그 외는 `INVALID_TOKEN`으로 401이다.
3. Redis에서 `refresh:{memberId}:{jti}`를 삭제한다. 이미 없어도 성공으로 처리한다(멱등).
4. 유효한 Access Token이 함께 왔고 Refresh Token과 같은 회원의 것이면, 그 `jti`를 `blacklist:{jti}`로 Redis에 저장한다. TTL은 Access Token의 남은 만료 시간이다.
5. 다른 기기의 Refresh Token은 그대로이다(다중 로그인 유지).

## 블랙리스트

- `JwtRepository`에 `saveBlacklist`, `existsBlacklist`를 추가한다.
- `JwtAuthenticationFilter`: 토큰 검증 뒤 `JwtCommandHandler`로 블랙리스트 여부를 확인하고, 있으면 `INVALID_TOKEN` 401로 처리한다. 인증이 필요한 모든 요청에서 Redis 조회가 1회 늘어난다.
- Redis가 장애일 때는 인증이 필요한 요청이 모두 500이 되는 방식(fail-closed)을 추천한다. 보안상 안전한 쪽이다.
- `AuthenticatedMember`에 토큰 `jti`와 만료 일시를 추가해서 로그아웃 필터가 블랙리스트에 올릴 때 사용한다.

## 구성

- `LogoutFilter`: 위 동작을 처리하고 `JwtCommandHandler.logout`을 직접 호출한다(로그인 필터와 같은 예외 구조).
- `JwtCommandHandler`: Refresh Token 폐기와 Access Token 블랙리스트 등록 메서드
- `SecurityConfig`: `/v1/logout`을 `permitAll`에 추가하고 필터를 연결한다.
- OpenAPI(`jwt` 문서): `/v1/logout` 추가. 응답 200, 400, 401(`TOKEN_EXPIRED`, `INVALID_TOKEN`), 500

## 테스트

- `JwtRepositoryTest`: 블랙리스트 저장·조회·TTL
- `LogoutTest`(Postgres + Redis)
  - 로그아웃 후 Refresh Token 키 삭제, 그 토큰으로 재발급하면 `INVALID_TOKEN`
  - 로그아웃한 Access Token으로 보호된 주소를 호출하면 `INVALID_TOKEN` 401, 블랙리스트 TTL이 남은 만료 시간 안팎
  - 두 번 로그인한 뒤 한쪽만 로그아웃하면 다른 쪽 Refresh Token은 유효
  - Authorization 헤더 없이도, 만료되거나 잘못된 Access Token이 있어도 로그아웃 성공
  - 다른 회원의 Access Token을 함께 보내면 Refresh Token만 폐기되고 그 Access Token은 블랙리스트에 오르지 않음
  - 같은 Refresh Token으로 두 번 로그아웃해도 200(멱등)
  - 만료·잘못된 Refresh Token 401, 빈 값·JSON이 아닌 본문 400, GET은 401

## 정해야 할 것

각 항목에 추천안을 같이 적었다.

1. 로그아웃을 직접 만든 `LogoutFilter`(추천)로 할지, Spring `logout()`으로 할지
2. 인증 기준을 Refresh Token 필수 + Access Token 선택(추천)으로 할지, Access Token 필수로 할지
3. 유효하지 않거나 만료된 Refresh Token은 401(추천)로 할지, 멱등하게 200으로 할지
4. 블랙리스트 확인은 Redis 장애 시 fail-closed(추천)로 할지
5. 블랙리스트된 토큰의 오류 코드는 `INVALID_TOKEN`(추천)으로 할지, `TOKEN_REVOKED`처럼 새로 만들지(클라이언트가 재로그인으로만 반응하면 구분이 필요 없다)

---

# 4단계 구현 후 API 문서 점검과 정리 제안

> api 문서 확인
>
> 1. jwt controller 없어졌는데 api 명세서에는 Jwt 도메인이 있음 이거 바꿔야하는거 아님?
> 2. 베어러 선택헤더로 추가
> 3. security/handler에 common으로 시작하는애들 common 빼도 되지 않아? 처음에 common에 만들어서 붙어있는거 같은데
> 4. jwt의 jwtclaims, issuedtoken 이런애들 다 데이터 객체같은데 jwt/v1하위에 적절한 위치에 저장하는거불가능한가?

## 답변

4단계 구현이 끝난 뒤 API 문서를 점검하고, 사용자가 지적한 정리 사항 네 가지에 답했다. 2번은 바로 반영했고 나머지는 제안과 확인을 거쳤다.

## 점검 결과

| 항목 | 결과 |
|---|---|
| OpenAPI 3.1 규격 검증 (`openapi-spec-validator`를 임시로 설치해 실행한 뒤 삭제) | `member`, `jwt` 문서 모두 통과 |
| JSON 파싱, `$ref` 해석 | 끊어진 참조와 쓰지 않는 스키마 없음 |
| 예시와 스키마 | 모든 요청·응답 예시가 스키마의 필수 필드와 필드 이름에 맞음 |
| 예시 값과 코드 | 오류 코드명과 메시지가 코드와 일치 |
| 옛 경로·이름 | `/v1/jwt/...`, `/v1/auth/...`, `JwtTokenResponse`, `ReissueRequest`는 남아 있지 않음 |

문서의 API는 `member` 문서에 `POST /v1/member/join`(201, 400(2), 409(2), 500)과 `POST /v1/member/reissue`(200, 400(2), 401(2), 500), `jwt` 문서에 `POST /v1/login`(200, 400(3), 401, 500)과 `POST /v1/logout`(200, 400(2), 401(2), 500)이다.

보완할 점으로 지적한 것은 로그아웃의 `Authorization` 헤더가 설명 글에만 있고 헤더 파라미터로는 정의되지 않아 Apidog에서 입력란이 보이지 않는 것, 그리고 헤더만 다르고 본문이 똑같은 요청 예시 두 개였다. Apidog 가져오기는 확인하지 못했다.

## 로그아웃 헤더 추가 (반영함)

- 로그아웃에 헤더 파라미터 `Authorization`(`in: header`, `required: false`, 형식 `Bearer {토큰}`)을 추가했다. 헤더 설명과 예시("Access Token과 함께" / "헤더 없음")는 파라미터에 넣었다.
- 본문만 다른 것이 아니라서 헤더만 다르던 요청 예시 두 개는 하나("정상 요청")로 합쳤다.
- `member`, `jwt` 문서 모두 OpenAPI 3.1 검증을 통과했다.

## 문서의 jwt 도메인 이름

로그인과 로그아웃은 `security` 패키지의 필터가 처리하고 `jwt` 도메인에는 컨트롤러가 없어졌으니 문서 이름이 실제 코드 위치와 어긋난다는 지적이 맞다. 이름을 `security`로 바꾸는 것을 추천했다. 대안인 `auth`는 이미 없앤 도메인 이름이라 혼동될 수 있다.

## Common 접두어 제거 제안

`CommonAuthenticationEntryPoint`, `CommonAccessDeniedHandler`는 처음에 `common/security` 아래에 만들어서 붙은 이름이 맞다. 다만 접두어만 빼면 이름이 Spring 인터페이스(`AuthenticationEntryPoint`, `AccessDeniedHandler`)와 같아져서 구현하는 쪽에서 인터페이스를 패키지까지 풀어 써야 하고 읽기가 불편하다. 그래서 `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`를 제안했다. `LoginSuccessHandler`, `LoginFailureHandler`, `SecurityResponseWriter`는 그대로 둔다.

## 데이터 객체 위치 제안

`JwtClaims`, `IssuedToken`, `IssuedTokens`, `TokenType`은 데이터 객체이므로 `jwt/v1` 아래로 옮기는 것이 가능하다. 안은 다음과 같다.

| 클래스 | 이동 |
|---|---|
| `JwtClaims`, `IssuedToken`, `IssuedTokens` | `jwt/v1/command/dto` |
| `TokenType`(enum) | `jwt/v1/command/type` |
| `JwtProvider`, `JwtProperties` | `jwt/` 루트에 유지(토큰 생성·검증 컴포넌트와 설정 객체) |

도메인 모듈의 구조(`command/{entity,dto,type}`)를 `member-api`의 `jwt` 도메인에도 따르는 안이다. 패키지 이름과 import만 바뀌고 동작은 같으며 영향받는 파일은 15개 안팎이다.

---

# 문서 이름 결정과 핸들러 이름, 데이터 객체 위치

> 1. 토큰 재발급은 member에 있으니 member, 로그인/로그아웃은 일단 security ㅇㅋ
> 2. rest도 빼고 그냥 하면 안되나?
> 3. jwt/v1/command/payload/dto, type 별로인가?

## 답변

문서 이름은 결정되어 바로 반영했고, 핸들러 이름과 데이터 객체 위치는 의견을 말하고 확인을 받기로 했다.

## 문서 이름 변경 (반영함)

- 토큰 재발급은 이미 `member` 문서(`/v1/member/reissue`)에 있고, 로그인과 로그아웃은 `security`로 정했다.
- `choimory-dev-member-api-jwt-v1.json`을 `choimory-dev-member-api-security-v1.json`으로 바꿨다. 제목, 태그, 상단 설명을 `security` 기준으로 고쳤고 설명에는 "Spring Security 필터가 처리하는 로그인과 로그아웃을 다루고, 재발급은 컨트롤러라 `member` 문서에 있다"고 적었다.
- `docs/api/v1`에는 `member`(회원가입, 재발급)와 `security`(로그인, 로그아웃) 두 문서가 있고, `jwt` 문자열은 문서에 남아 있지 않다.

## 접두어를 모두 빼는 경우

가능은 하다. 다만 `AuthenticationEntryPoint`, `AccessDeniedHandler`가 구현하는 Spring 인터페이스와 이름이 같아서 다음처럼 된다.

```java
public class AuthenticationEntryPoint
        implements org.springframework.security.web.AuthenticationEntryPoint { ... }
```

- 구현하는 쪽에서 인터페이스를 패키지까지 풀어 써야 한다.
- IDE 자동 import가 Spring 것을 제안하기 쉽고, 로그와 스택 트레이스에서도 어느 쪽인지 헷갈린다.

접두어 없이 쓰려면 인터페이스와 겹치지 않게 역할로 짓는 편이 낫다. `CommonAuthenticationEntryPoint`(401)는 `UnauthorizedHandler`, `CommonAccessDeniedHandler`(403)는 `ForbiddenHandler`로 하는 안이다. 짧고 접두어가 필요 없으며 `LoginSuccessHandler`, `LoginFailureHandler`와 이름 스타일도 맞다.

## payload 아래에 두는 안

별로라고 봤다. 이 프로젝트에서 `payload`는 API로 주고받는 요청·응답 객체(`payload/request`, `payload/response`)를 뜻한다. `JwtClaims`, `IssuedToken(s)`은 API로 오가지 않는 내부 데이터이고 enum인 `TokenType`은 더욱 그렇다. `payload` 안에 두면 의미가 섞인다. 같은 단계의 `dto`, `type`을 권했고, 이렇게 하면 `payload`는 비게 되어 필요 없으면 삭제한다.

```
jwt/v1/command/
├── dto/        JwtClaims, IssuedToken, IssuedTokens
├── type/       TokenType
├── payload/    (비어 있음. 필요 없으면 삭제)
├── handler/, repository/
```

---

# 데이터 객체를 member-domain 모듈에 두는 안

> member-api말고 Member-domain 모듈은 어때?

## 답변

가능하지만 이번에는 추천하지 않았다. `member-api`의 `jwt/v1/command/dto`, `type`에 두는 쪽이 낫다고 판단했다.

## 되는 점

- 이 객체들은 Spring이나 jjwt에 의존하지 않는 단순 record와 enum이라 기술적으로는 어느 모듈에 둬도 컴파일된다.
- `member-domain`에도 엔티티가 아닌 객체(`CommonResponse`, `CommonValidateResult`)가 `common/command/response`에 있어서 선례가 없지는 않다.

## 맞지 않는 점

1. `command/dto` 규칙과 충돌한다. README에 `member-domain`의 `command/dto`는 엔티티와 1:1인 DTO 전용이고 `DtoRulesTest`가 이를 검사한다고 적혀 있다. `JwtClaims`, `IssuedToken(s)`는 엔티티 DTO가 아니라서 그 자리에 둘 수 없고, `common/command/response`처럼 별도 위치를 새로 만들어야 한다.
2. `type` 위치의 의미가 다르다. 도메인 모듈의 `type`은 DB 컬럼에 저장되는 enum(`MemberStatus`, `AuthorityCode` 등)이고, 변경하면 기존 데이터가 깨지는 값이라는 주의를 달아 둔다. `TokenType`은 토큰 안의 클레임 값일 뿐 DB에 저장되지 않는다.
3. 쓰는 곳이 `member-api`뿐이다. 토큰을 만들고 검증하는 `JwtProvider`, 필터, 핸들러가 모두 `member-api`에 있어서 데이터 객체만 다른 모듈로 빼면 모듈 경계를 오가며 수정해야 해서 이득이 없다.
4. 영속 도메인 모듈이 토큰 개념을 알게 된다. `member-domain`은 DDL, 엔티티, DTO 같은 저장 모델 중심이고 토큰은 API 계층의 관심사이다.

## 옮기는 쪽이 맞는 경우

나중에 이 토큰을 다른 서비스나 모듈이 읽어야 할 때이다. 예를 들어 다른 서버가 `JwtClaims` 형태로 토큰을 해석해야 하는 경우이다. 그때 해당 객체만 공유 모듈로 올리면 된다. 지금은 해당되지 않아서 필요할 때 옮기는 편이 낫다.

## 위치별 평가

| 안 | 평가 |
|---|---|
| `member-api`의 `jwt/v1/command/dto`, `type` | 추천 |
| `member-api`의 `jwt/v1/command/payload/dto`, `payload/type` | `payload` 의미가 섞여서 비추천 |
| `member-domain` | 지금은 비추천. 다른 모듈이 쓰게 되면 그때 이동 |

## 미정 사항

1. 데이터 객체 위치를 `jwt/v1/command/dto`, `type`으로 옮길지
2. 핸들러 이름을 `UnauthorizedHandler`, `ForbiddenHandler`로 할지(다른 이름이 좋으면 알려 주기)
