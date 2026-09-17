# 목차

- [개요](#개요)
- [완료 내용](#완료-내용)
- [검증 결과](#검증-결과)
- [남은 후속 작업](#남은-후속-작업)

---

# 개요

- Kotlin Java21 변환작업을 마무리했다.
- Step 1에서는 Kotlin 소스를 제거하고 Java 21 기반 Spring Boot 프로젝트로 초기화했다.
- Step 2에서는 Git 히스토리의 Kotlin 원본을 기준으로 기존 도메인, 설정, 보안, 테스트 보조 코드를 Java로 이관했다.
- 이후 rules 기준 코드 컨벤션 점검과 정리 작업까지 진행했다.

---

# 완료 내용

> 작업마무리

## 답변

Kotlin Java21 변환작업의 구현, 컨벤션 정리, 검증, README 반영을 완료했다.

## Step 1. Java 21 베이스 프로젝트 초기화

- Java 21 toolchain 기반 Gradle 프로젝트로 변경했다.
- Kotlin Gradle 플러그인과 Kotlin 전용 의존성을 제거했다.
- Kotlin 소스 디렉토리를 제거했다.
- 최소 Java Spring Boot 메인 클래스를 생성했다.
- Gradle 설정을 Kotlin DSL에서 Groovy DSL로 변경했다.

## Step 2. Kotlin 로직 Java 이관

- Common 객체와 공통 예외 처리를 Java로 이관했다.
- Member command 도메인, Repository, Handler, Service, Controller를 Java로 이관했다.
- Member query 도메인, Repository, Client, Handler, Service, Controller를 Java로 이관했다.
- Login/Security Filter, Provider, Handler를 Java로 이관했다.
- TokenDetails를 Java로 이관했다.
- App, Redis, Elasticsearch, Security 설정을 Java로 이관했다.
- 테스트 보조 클래스를 Java로 이관했다.
- Elasticsearch Java API Client 직접 버전 선언을 제거하고 Spring Data가 관리하는 버전을 사용하도록 정리했다.

## 코드 컨벤션 정리

- `sign-up` 요청은 `@ModelAttribute`로 정리했다.
- `verify` 요청은 `@RequestBody`로 정리했다.
- `CommonResponse`에 정적 팩토리를 추가했다.
  - `of(...)`
  - `ok(...)`
  - `created(...)`
  - `error(...)`
- Response 객체에 정적 팩토리를 추가했다.
  - `CreateMemberResponse.of(...)`
  - `VerifyMemberResponse.of(...)`
  - `LoginResponse.of(...)`
- 주요 호출부의 직접 생성자 호출을 정적 팩토리 호출로 변경했다.
- `TokenDetails.from(...)`을 추가했다.
- `MemberDocumentDto`를 `MemberDocument`와 1:1 필드 매칭되도록 확장했다.
- `FindMemberRequest`, `FindMemberResponse`를 `final class`로 변경했다.

## README 반영

- README를 Java 21 회원 API 구조 기준으로 갱신했다.
- 아키텍처, API 흐름, 코드 규칙, 주의 사항, 검증 명령을 반영했다.

---

# 검증 결과

## compileJava

```text
./gradlew compileJava
BUILD SUCCESSFUL
```

## compileTestJava

```text
./gradlew compileTestJava
BUILD SUCCESSFUL
```

## test

```text
./gradlew test
BUILD SUCCESSFUL
```

---

# 남은 후속 작업

- access token, refresh token 실제 생성 로직 구현
- 로그인 성공 시 Redis 토큰 등록 구현
- 회원가입 인증 메일 Kafka 발송 구현
- 함수 단위 테스트 작성
- API 단위 통합 테스트 작성
- 로컬 실행용 DB, Redis, Elasticsearch 환경 구성 정리
