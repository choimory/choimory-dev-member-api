# 목차

- [개요](#개요)
- [Step 1 구현 내용](#step-1-구현-내용)
- [Gradle Groovy DSL 전환](#gradle-groovy-dsl-전환)

---

# 개요

- Kotlin 프로젝트 Java 21 전환 작업의 Step 1을 진행했다.
- Step 1의 목표는 기존 Kotlin 로직을 이관하지 않고 Java 21 기반 빈 Spring Boot 프로젝트로 초기화하는 것이다.
- 기존 Kotlin 원본은 Step 1 이전 Git 커밋에서 참조하는 방식으로 결정했다.
- 기존 API 기능 복원은 Step 2에서 별도로 진행한다.

---

# Step 1 구현 내용

> 구현 시작

## 답변

Step 1 구현을 진행했다.

## 변경 내용

### CALENDAR 상태 변경

- `.agents/CALENDAR.md`의 작업 상태를 변경했다.
- 기존 상태는 `PLAN:Kotlin Java21 변환작업(kotlin-to-java21-migration)`이었다.
- 변경 후 상태는 `TASK:Kotlin Java21 변환작업(kotlin-to-java21-migration)`이다.

### Gradle Java 21 전환

- `build.gradle.kts`를 Java 21 기준으로 정리했다.
- `java` 플러그인을 명시했다.
- Java toolchain을 17에서 21로 변경했다.
- Kotlin Gradle 플러그인을 제거했다.
  - `kotlin("jvm")`
  - `kotlin("plugin.spring")`
  - `kotlin("plugin.jpa")`
- Ktlint 플러그인을 제거했다.
  - `org.jlleitschuh.gradle.ktlint`
- Kotlin 전용 의존성을 제거했다.
  - `com.fasterxml.jackson.module:jackson-module-kotlin`
  - `org.jetbrains.kotlin:kotlin-reflect`
  - `io.github.oshai:kotlin-logging-jvm`
  - `org.jetbrains.kotlin:kotlin-test-junit5`
- Kotlin 컴파일 설정을 제거했다.
  - `kotlin { compilerOptions { ... } }`
- Kotlin JPA open 설정을 제거했다.
  - `allOpen { ... }`

### 유지한 의존성

- Spring Boot Data JPA
- Spring Security
- Spring Validation
- Spring Web
- Spring Data Elasticsearch
- Elasticsearch Java API Client
- Spring Data Redis
- Jackson Databind
- UUID Creator
- Lombok
- PostgreSQL Driver
- Spring Boot Configuration Processor
- Spring Boot Test
- Spring Boot Testcontainers
- Spring Security Test
- Testcontainers JUnit Jupiter
- Testcontainers PostgreSQL
- JUnit Platform Launcher

### Kotlin 소스 삭제

- 기존 Kotlin 소스 디렉토리를 삭제했다.
  - `src/main/kotlin`
  - `src/test/kotlin`
- Step 2에서 필요한 Kotlin 원본은 Git 히스토리의 기준 커밋에서 참조한다.

### Java 실행 클래스 생성

- 최소 Spring Boot 실행 클래스를 생성했다.
- 생성 파일은 `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java`다.
- 기존 루트 패키지인 `dev.choimory.member.api`를 유지했다.
- `@SpringBootApplication`을 적용했다.
- 향후 JPA Entity 이관을 고려해 `@EnableJpaAuditing`을 유지했다.

## 현재 상태

- 프로젝트는 Java 21 기반 빈 Spring Boot 프로젝트 구조가 되었다.
- 기존 Kotlin API 로직은 워킹트리에서 제거되었다.
- 기존 resources는 유지했다.
  - `application.yml`
  - profile yml
  - SQL 파일
- `.kt` 파일은 남아 있지 않다.
- `.kts` 파일은 Gradle 설정 파일만 남아 있다.

## 검증 상태

- `./gradlew compileJava`는 아직 실행하지 않았다.
- 테스트도 실행하지 않았다.
- 검증 명령 실행은 별도 허락 후 진행한다.

## 변경 파일 요약

- 수정
  - `.agents/CALENDAR.md`
  - `build.gradle.kts`
- 추가
  - `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java`
- 삭제
  - `src/main/kotlin`
  - `src/test/kotlin`

---

# Gradle Groovy DSL 전환

> gradle kt말고 다른거로 하고 싶음

## 답변

Gradle 설정을 Kotlin DSL에서 Groovy DSL로 전환할 수 있다고 판단했다.

`build.gradle.kts`를 `build.gradle`로 바꾸고, `settings.gradle.kts`도 `settings.gradle`로 바꾸면 Gradle 설정 파일에서도 Kotlin DSL이 사라진다.

## 진행 내용

> 진행해줘

Gradle 설정을 Groovy DSL로 전환했다.

## 변경 내용

- `build.gradle.kts` 내용을 Groovy DSL로 변환했다.
- `settings.gradle.kts` 내용을 Groovy DSL로 변환했다.
- `build.gradle`을 생성했다.
- `settings.gradle`을 생성했다.
- `build.gradle.kts`를 삭제했다.
- `settings.gradle.kts`를 삭제했다.

## 유지한 설정

- Java plugin
- Spring Boot plugin
- Spring Dependency Management plugin
- Java 21 toolchain
- Maven Central repository
- Spring snapshot repository
- 기존 Java/Spring 기준 의존성 구성
- JUnit Platform 테스트 설정

## 현재 상태

- 남아 있는 `.kts` 파일은 없다.
- Gradle 설정 파일은 다음 두 개다.
  - `build.gradle`
  - `settings.gradle`

## 검증 상태

- Gradle 검증 명령은 아직 실행하지 않았다.
- `./gradlew compileJava`는 별도 허락 후 실행한다.
