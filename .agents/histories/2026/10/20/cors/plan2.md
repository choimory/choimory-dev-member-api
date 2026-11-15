# 목차

- [개요](#개요)
- [corsproperties를 config.properties에 놓는게 어때](#corsproperties를-configproperties에-놓는게-어때)
  - [이유](#이유)
  - [함께 생각해 볼 점](#함께-생각해-볼-점)
  - [변경 범위](#변경-범위)
- [jwtproperties도 config.properties에 놓는건 어때?](#jwtproperties도-configproperties에-놓는건-어때)
  - [이유](#이유-1)
  - [영향 범위](#영향-범위)
  - [작업 순서](#작업-순서)

---

# 개요

- task1.md에서 구현한 `CorsProperties`와 기존 `JwtProperties`의 패키지 위치를 `config.properties`로 정리하는 방안을 분석한다.
- 구현은 아직 시작하지 않았다.

---

# corsproperties를 config.properties에 놓는게 어때

> corsproperties를 config.properties에 놓는게 어때

## 답변

좋은 안이다. 찬성한다.

## 이유

- `CorsProperties`는 `CorsConfig`나 `SecurityConfig`가 쓰는 설정값 객체라서, `config` 아래에 `properties` 패키지를 두고 설정 바인딩 객체를 모아 두면 찾기 쉽다.
- 지금 `config` 패키지에는 `JpaConfig`, `PasswordEncoderConfig`, `SecurityConfig`, `CorsConfig`와 `CorsProperties`가 섞여 있다. 분리하면 `@Configuration` 클래스와 프로퍼티 레코드가 구분된다.
- 이후 프로퍼티가 늘어도 같은 곳에 둘 수 있다.

## 함께 생각해 볼 점

- `JwtProperties`는 `jwt/v1/command/payload/dto`에 있어서 위치 기준이 맞지 않게 된다. 둘 다 `config.properties`로 옮기는 게 일관된다.
- 이 시점에는 JWT 쪽 import와 테스트까지 건드리는 범위가 커 보여, `CorsProperties`만 먼저 옮기고 `JwtProperties`는 별도로 두는 안을 추천했다.
- 패키지 이름은 `properties`가 적당하다.

## 변경 범위

`CorsProperties`만 옮길 경우의 범위는 다음과 같다.

1. `CorsProperties`를 `config/properties/`로 이동하고 `package` 선언을 `dev.choimory.member.api.config.properties`로 바꾼다.
2. `CorsConfig`, `SecurityConfig`, `CorsPropertiesTest`의 import를 수정한다. `CorsConfig`와 `SecurityConfig`는 기존에 같은 패키지라 import가 없었다.
3. `CorsPropertiesTest`를 `config/properties/`로 옮기는 안을 추천한다.
4. `task1.md`의 파일 경로와 설명을 새 위치로 고친다.
5. README의 `config` 목록과 구조 설명에 새 패키지를 반영한다. 마무리 단계에서 한다.

---

# jwtproperties도 config.properties에 놓는건 어때?

> jwtproperties도 config.properties에 놓는건 어때?

## 답변

함께 옮기는 안에 찬성한다. 처음에는 범위가 커서 따로 하자고 했지만, 영향 범위를 확인해 보니 작았다.

## 이유

- `JwtProperties`는 도메인 로직이 아니라 `application-jwt.yml`을 바인딩하는 설정값 객체다. 지금 `jwt/v1/command/payload/dto`에 있는 건 DTO 규칙에 끼워 맞춘 위치에 가깝다. DTO는 Entity와 1:1로 매칭해야 하는데 이 레코드는 해당되지 않는다.
- `CorsProperties`만 옮기면 같은 성격의 클래스가 두 곳에 흩어진다. 함께 옮기면 `config.properties`가 설정 바인딩 객체의 단일 위치가 된다.

## 영향 범위

- 메인 코드: `JwtProvider`의 import 1곳, `SecurityConfig`의 import 1곳. `JwtProperties` 자체는 `package` 선언만 바꾼다.
- 테스트: `JwtProviderTest`, `LogoutTest`, `MemberReissueTest`, `JwtAuthenticationFilterTest` 4곳의 import 수정
- `jwt/.../payload/dto` 디렉토리에 `JwtProperties` 외 다른 파일이 있는지 확인하고, 비면 삭제한다.
- `.agents/histories/2026/10/19/member-join-security-auth`의 과거 문서(plan, task, done)에도 `JwtProperties` 경로가 언급된다. 과거 히스토리는 그 시점의 기록이므로 수정하지 않는다.

## 작업 순서

1. `CorsProperties`와 `JwtProperties`를 `config/properties/`로 옮기고 `package` 선언을 수정한다.
2. 위 파일들의 import를 수정한다.
3. `CorsPropertiesTest`를 `config/properties/`로 옮긴다.
4. 전체 테스트를 실행한다.
5. `task1.md`의 경로와 설명을 새 위치로 고치고, 이동 내용을 질문과 답변 형식으로 추가한다. README 반영은 마무리 단계에서 한다.

---
