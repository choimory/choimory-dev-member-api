# 목차

- [개요](#개요)
- [시작](#시작)
  - [변경 내용](#변경-내용)
  - [테스트 결과](#테스트-결과)

---

# 개요

- plan2.md의 분석에 따라 설정 바인딩 객체인 `CorsProperties`와 `JwtProperties`를 `config.properties` 패키지로 옮긴다.
- task1.md의 구현 이후 이어지는 패키지 정리 작업이다.

---

# 시작

> 시작

## 답변

plan2.md의 분석대로 `CorsProperties`와 `JwtProperties`를 `config.properties` 패키지로 옮겼다.

## 변경 내용

- `JwtProperties`: `jwt/v1/command/payload/dto/`에서 `config/properties/`로 이동했다. (`git mv`) `package`를 `dev.choimory.member.api.config.properties`로 바꿨다.
- `CorsProperties`: `config/`에서 `config/properties/`로 이동하고 `package`를 바꿨다.
- `CorsPropertiesTest`: `config/`에서 `config/properties/`로 이동하고 `package`를 바꿨다.
- import 수정
  - 메인: `JwtProvider`, `SecurityConfig`(Jwt, Cors 모두), `CorsConfig`(Cors)
  - 테스트: `JwtProviderTest`, `LogoutTest`, `MemberReissueTest`, `JwtAuthenticationFilterTest`
- `jwt/v1/command/payload/dto/`에는 `IssuedToken`, `IssuedTokens`, `JwtClaims`가 남아 있어 디렉토리는 유지했다.
- `./gradlew spotlessApply`로 import 정렬을 맞췄다.
- task1.md는 이동 전 구현 기록이므로 당시 경로(`config/CorsProperties.java`, `config/CorsPropertiesTest.java`)를 그대로 둔다. `.agents/histories/2026/10/19`의 과거 문서도 수정하지 않았다.

## 테스트 결과

- 전체 테스트가 통과했다. (`./gradlew test`, BUILD SUCCESSFUL)
- README 반영은 작업 마무리 단계에서 한다.

---
