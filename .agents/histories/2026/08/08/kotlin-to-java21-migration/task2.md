# 목차

- [개요](#개요)
- [Step 2 구현 내용](#step-2-구현-내용)
- [Java 이관 상세](#java-이관-상세)
- [검증](#검증)
- [주의 및 보정 사항](#주의-및-보정-사항)

---

# 개요

- Kotlin Java21 변환작업의 Step 2를 이어서 진행했다.
- Step 1에서 Java 21 빈 프로젝트로 초기화된 상태에서, Git 히스토리의 Kotlin 원본을 기준으로 도메인 로직을 Java로 이관했다.
- Kotlin 원본 기준 커밋은 `bee3035`다.
- 이관 대상은 `src/main/kotlin`, `src/test/kotlin`에 있던 41개 Kotlin 파일이다.
- 이 문서는 Step 2 구현 중 진행한 작업 내용을 기록한다.

---

# Step 2 구현 내용

> 마저 진행해보자

## 답변

Step 2 계획인 기존 Kotlin 로직 Java 이관을 진행했다.

## 기준

- `.agents/histories/2026/08/08/kotlin-to-java21-migration/plan3.md`의 이관 순서와 파일 매핑을 기준으로 했다.
- 기존 Kotlin 코드는 `git show bee3035:<파일경로>` 방식으로 참조했다.
- Java 패키지 루트는 `dev.choimory.member.api`를 유지했다.
- 기존 Kotlin TODO는 새 기능 구현 없이 가능한 범위에서 유지했다.

---

# Java 이관 상세

## Common

- 공통 일시 Entity와 Document를 Java로 이관했다.
- 공통 validation 결과 DTO를 Java로 이관했다.
- 공통 응답 객체 `CommonResponse<T>`를 Java generic 객체로 이관했다.
- 공통 예외 `CommonException`을 Java RuntimeException 기반으로 이관했다.
- 공통 예외 핸들러를 Java로 이관하고 Kotlin logging 대신 SLF4J Lombok 로깅을 사용했다.
- `TimeUnitUtil`의 companion object 함수는 Java static method로 이관했다.

## Member Command

- `MemberEntity`를 JPA Entity로 이관했다.
- `MemberEntityDto`를 불변 DTO로 이관했다.
- 회원가입 요청, 회원 인증 요청, 회원가입 응답, 회원 인증 응답 객체를 Java로 이관했다.
- `MemberCommandRepository`를 Java interface로 이관했다.
- `MemberCommandHandler`를 Java component로 이관했다.
- `MemberCommandService`를 Java service로 이관했다.
- `MemberCommandController`를 Java REST controller로 이관했다.

## Member Query

- `MemberDocument`를 Elasticsearch Document로 이관했다.
- `MemberDocumentDto`를 Java DTO로 이관했다.
- 기존 Kotlin `MemberDocumentDto.from()`은 TODO였으나 로그인 흐름 복원을 위해 `email` 매핑으로 구현했다.
- `FindMemberRequest`, `FindMemberResponse`는 기존처럼 빈 객체로 이관했다.
- `MemberQueryRepository`, `MemberQueryClient`, `MemberQueryHandler`, `MemberQueryService`, `MemberQueryController`를 Java로 이관했다.

## Login, Token, Security

- `TokenDetails`를 Java 불변 객체로 이관했다.
- `LoginRequest`는 기존 Kotlin package 불일치를 정리해 `dev.choimory.member.api.login.v1.domain.request`로 이관했다.
- `LoginResponse`를 Java 불변 객체로 이관했다.
- `LoginAuthProvider`, `LoginProcessFilter`, `LoginSuccessHandler`, `LoginFailureHandler`를 Java로 이관했다.
- 기존 토큰 생성 TODO는 유지하고 placeholder 토큰 응답을 유지했다.

## Config

- `AppConfig`를 Java 설정 클래스로 이관했다.
- `RedisConfig`를 Java 설정 클래스로 이관했다.
- `ElasticSearchConfig`를 Java 설정 클래스로 이관했다.
- `SecurityConfig`를 Java 설정 클래스로 이관했다.

## Test

- `ChoimoryDevMemberApiApplicationTests`를 Java로 이관했다.
- `TestChoimoryDevMemberApiApplication`을 Java로 이관했다.
- `TestcontainersConfiguration`을 Java로 이관했다.
- 테스트 컨텍스트에서 Elasticsearch, Redis placeholder가 없어서 실패하던 문제를 테스트 속성 기본값으로 보정했다.

## README

- README의 빈 프로젝트 설명을 제거했다.
- 현재 Java 도메인 구조, API 흐름, 코드 규칙, 주의 사항, 검증 명령을 반영했다.

---

# 검증

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

초기 테스트는 `es-host` placeholder 누락으로 실패했다.

```text
Could not resolve placeholder 'es-host' in value "${es-host}"
```

테스트 클래스에 ES/Redis placeholder 기본값을 추가한 뒤 다시 실행했다.

```text
./gradlew test
BUILD SUCCESSFUL
```

---

# 주의 및 보정 사항

## Elasticsearch Java API Client 버전 충돌

테스트 성공 전 shutdown 과정에서 다음 경고가 발생했다.

```text
NoSuchMethodError: 'void co.elastic.clients.ApiClient.close()'
```

원인은 `build.gradle`에서 `co.elastic.clients:elasticsearch-java:8.12.2`를 직접 선언해 Spring Data Elasticsearch가 기대하는 `8.18.1`이 낮은 버전으로 강제된 것이다.

해결을 위해 직접 의존성을 제거하고 Spring Boot/Spring Data가 관리하는 버전을 사용하도록 했다.

`dependencyInsight` 결과 `elasticsearch-java:8.18.1`이 선택되는 것을 확인했다.

## 기존 TODO 유지

- access token, refresh token 생성은 아직 placeholder 응답이다.
- Redis 토큰 등록도 TODO 상태다.
- 이메일 발송 Kafka도 TODO 상태다.

## 기존 Kotlin 대비 보정

- `LoginRequest` package 불일치를 Java 이관 시 정상 패키지로 정리했다.
- `MemberCommandHandler.getWaitVerifyMember()`에서 요청 이메일과 Redis DTO 이메일이 일치하는지 확인하도록 최소 보정했다.
- `MemberDocumentDto.from()`은 로그인 흐름 복원을 위해 이메일 매핑을 구현했다.
