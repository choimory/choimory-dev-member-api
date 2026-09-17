# 목차

- [개요](#개요)
- [Step 1 목표](#step-1-목표)
- [작업 범위](#작업-범위)
- [파일 처리 계획](#파일-처리-계획)
- [Kotlin 원본 보존 정책](#kotlin-원본-보존-정책)
- [Git 확인 결과](#git-확인-결과)
- [Gradle 변경 계획](#gradle-변경-계획)
- [유지할 의존성](#유지할-의존성)
- [제거할 구성](#제거할-구성)
- [생성할 Java 기본 클래스](#생성할-java-기본-클래스)
- [리소스 처리 계획](#리소스-처리-계획)
- [검증 계획](#검증-계획)
- [완료 기준](#완료-기준)
- [보류 사항](#보류-사항)
- [Step 1 시작 전 실행 계획](#step-1-시작-전-실행-계획)

---

# 개요

- Step 1은 Java 21 베이스 프로젝트 초기화 작업이다.
- 기존 Kotlin 소스의 로직은 Java로 이관하지 않는다.
- 기존 프로젝트에서 사용하던 주요 라이브러리 구성만 Java 기준으로 재구성한다.
- 기존 API 기능 복원은 Step 2에서 별도 작업으로 진행한다.

---

# Step 1 목표

- Java 21 기반 Spring Boot 프로젝트 구조를 만든다.
- Kotlin 플러그인과 Kotlin 전용 의존성을 제거한다.
- Java 소스 디렉토리와 최소 실행 클래스를 구성한다.
- 기존 기능 라이브러리는 Java 프로젝트에서 사용할 수 있도록 유지한다.
- 목표 결과물은 기존 API 기능이 없는 빈 Java 21 백엔드 프로젝트다.

---

# 작업 범위

- `build.gradle.kts`를 Java 21 기준으로 변경한다.
- `src/main/java` 디렉토리를 만든다.
- `src/test/java` 디렉토리를 만든다.
- 최소 Spring Boot 실행 클래스를 Java로 작성한다.
- 필요 시 최소 context load 테스트 클래스를 Java로 작성한다.
- Kotlin 소스의 Entity, DTO, Repository, Service, Controller, Security 로직은 이관하지 않는다.

---

# 파일 처리 계획

## 생성 대상

- `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java`
- 필요 시 `src/test/java/dev/choimory/member/api/ChoimoryDevMemberApiApplicationTests.java`

## 제거 또는 제외 대상

- `src/main/kotlin`
- `src/test/kotlin`

## 처리 방식 결정

- Step 1에서는 Kotlin 소스를 삭제한다.
- Kotlin 소스는 저장소 내부 보관 디렉토리로 옮기지 않는다.
- Step 2에서 필요한 Kotlin 원본은 Step 1 이전 Git 커밋에서 참조한다.
- 따라서 Step 1 시작 전에 사용자가 현재 상태를 직접 커밋해야 한다.

---

# Kotlin 원본 보존 정책

- Kotlin 원본 보존 방식은 Git 히스토리 참조 방식으로 결정한다.
- `src/main/kotlin`, `src/test/kotlin`은 Step 1에서 제거한다.
- `.agents` 아래에 Kotlin 원본 전체를 복사하거나 이동하지 않는다.
- Step 2에서는 Step 1 이전 커밋 해시를 기준으로 `git show <commit>:<path>` 형태로 Kotlin 원본을 확인한다.
- Git commit과 push는 사용자가 직접 진행하므로 Step 1 시작 전 현재 상태 커밋은 사용자가 수행한다.

## Step 1 시작 전 확인 사항

- 사용자가 현재 Kotlin 코드 상태를 커밋했는지 확인한다.
- 커밋 해시를 Step 2 참조 기준으로 기록할지 결정한다.
- 커밋하지 않은 상태에서는 Kotlin 소스 삭제 작업을 진행하지 않는다.

---

# Git 확인 결과

- 현재 브랜치는 `feature/conversion-to-java`다.
- 현재 HEAD는 `bee303584cfaf9d0ba71668df79687de1f2122f8`다.
- `src/main/kotlin`, `src/test/kotlin` 아래 Kotlin 파일 41개가 모두 Git에 추적 중이다.
- Kotlin 소스 쪽 uncommitted 변경은 없다.
- `git show HEAD:src/main/kotlin/dev/choimory/member/api/ChoimoryDevMemberApiApplication.kt`로 원본 조회가 가능했다.
- `git show HEAD:src/main/kotlin/dev/choimory/member/api/member/v1/command/domain/entity/MemberEntity.kt`로 원본 조회가 가능했다.
- 따라서 Step 1에서 Kotlin 소스를 삭제해도 Step 2에서 현재 HEAD 기준으로 Kotlin 원본을 참조할 수 있다.

## Step 1 실행 전 권장 사항

- Kotlin 원본만 기준으로 하면 현재 HEAD를 Step 2 원본 기준으로 사용할 수 있다.
- 현재 작성된 plan 문서들까지 기준점에 포함하려면 사용자가 Step 1 시작 전에 문서 변경사항을 직접 커밋하는 것이 좋다.
- Git commit과 push는 사용자가 직접 진행한다.

---

# Gradle 변경 계획

## plugins 변경

- 제거한다.
  - `kotlin("jvm") version "1.9.25"`
  - `kotlin("plugin.spring") version "1.9.25"`
  - `kotlin("plugin.jpa") version "1.9.25"`
  - `id("org.jlleitschuh.gradle.ktlint") version "12.1.0"`
- 유지한다.
  - `id("org.springframework.boot") version "3.5.4-SNAPSHOT"`
  - `id("io.spring.dependency-management") version "1.1.7"`
- 추가 또는 명시를 검토한다.
  - `java`

## java toolchain 변경

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

## configurations 유지

- Lombok annotation processor 사용을 위해 현재 `compileOnly` 설정은 유지한다.

## repositories 유지

- `mavenCentral()` 유지
- Spring Boot snapshot 버전을 계속 사용할 경우 `https://repo.spring.io/snapshot` 유지
- snapshot 버전을 안정 버전으로 바꿀지는 별도 결정한다.

---

# 유지할 의존성

- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-data-elasticsearch`
- `co.elastic.clients:elasticsearch-java:8.12.2`
- `org.springframework.boot:spring-boot-starter-data-redis`
- `com.fasterxml.jackson.core:jackson-databind:2.17.0`
- `com.github.f4b6a3:uuid-creator:5.3.2`
- `org.projectlombok:lombok`
- `org.postgresql:postgresql`
- `org.springframework.boot:spring-boot-configuration-processor`
- `org.springframework.boot:spring-boot-starter-test`
- `org.springframework.boot:spring-boot-testcontainers`
- `org.springframework.security:spring-security-test`
- `org.testcontainers:junit-jupiter`
- `org.testcontainers:postgresql`
- `org.junit.platform:junit-platform-launcher`

---

# 제거할 구성

## Kotlin 의존성

- `com.fasterxml.jackson.module:jackson-module-kotlin`
- `org.jetbrains.kotlin:kotlin-reflect`
- `io.github.oshai:kotlin-logging-jvm:5.1.1`
- `org.jetbrains.kotlin:kotlin-test-junit5`

## Kotlin Gradle 설정

- `kotlin { compilerOptions { ... } }`
- `allOpen { ... }`

## Ktlint

- `org.jlleitschuh.gradle.ktlint` 플러그인은 Java 프로젝트에서는 제거한다.
- Java formatter 도입 여부는 별도 작업으로 판단한다.

---

# 생성할 Java 기본 클래스

## Application

- 패키지는 기존 프로젝트 루트 패키지인 `dev.choimory.member.api`를 사용한다.
- `@SpringBootApplication`을 유지한다.
- JPA Auditing을 유지할지 여부는 Step 1에서 결정한다.
- 빈 프로젝트지만 추후 JPA Entity 이관을 고려하면 `@EnableJpaAuditing`을 유지할 수 있다.

예상 구조는 다음과 같다.

```java
package dev.choimory.member.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ChoimoryDevMemberApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChoimoryDevMemberApiApplication.class, args);
    }
}
```

---

# 리소스 처리 계획

## 유지 후보

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/application-datasource.yml`
- `src/main/resources/sql/schema.sql`
- `src/main/resources/sql/data.sql`

## 주의점

- 리소스를 그대로 유지하면 DB, Redis, Elasticsearch 관련 환경변수 누락으로 애플리케이션 부팅이 실패할 수 있다.
- Step 1의 목표를 컴파일 가능 상태로 둘지, 환경변수 없이 부팅 가능한 상태로 둘지 먼저 결정해야 한다.
- 환경변수 없이 부팅 가능한 빈 프로젝트가 목표라면 datasource profile include 또는 외부 연결 설정을 조정해야 한다.

---

# 검증 계획

- 구현 전에는 테스트나 빌드를 실행하지 않는다.
- 작업 시작 허락 후 Step 1 구현이 끝나면 별도 허락을 받고 컴파일 확인을 진행한다.
- 컴파일 확인 후보 명령은 `./gradlew compileJava`다.
- 테스트 확인 후보 명령은 `./gradlew test`다.
- Testcontainers는 Docker 환경 영향을 받으므로 테스트 실행 전 별도 확인이 필요하다.

---

# 완료 기준

- `build.gradle.kts`가 Java 21 기준으로 정리되어 있다.
- Kotlin 전용 플러그인과 의존성이 제거되어 있다.
- Java 기본 소스 디렉토리가 구성되어 있다.
- 최소 Spring Boot Application 클래스가 Java로 존재한다.
- 기존 Kotlin 로직은 Step 2 대상으로 분리되어 있다.
- 컴파일 확인을 진행했다면 결과가 문서 또는 작업 요약에 기록되어 있다.

---

# 보류 사항

- 리소스 파일 유지 여부
- Spring Boot snapshot 버전 유지 여부
- Java formatter 도입 여부
- context load 테스트 생성 여부
- 환경변수 없이 부팅 가능한 상태까지 Step 1에 포함할지 여부

---

# Step 1 시작 전 실행 계획

> step1 시작해볼까? 계획 이야기해봐

## 답변

Step 1은 “Java 21 베이스 프로젝트 초기화”로 진행한다.

바로 수정하지 않고, 실행 계획을 먼저 정리했다.

## Step 1 목표

- 기존 Kotlin 로직은 전부 제거한다.
- Java 21 기반 빈 Spring Boot 프로젝트로 정리한다.
- 기존 주요 라이브러리는 Java 기준으로 유지한다.
- 최소 실행 가능한 Java 애플리케이션 클래스를 생성한다.
- Step 2는 나중에 Git 히스토리의 Kotlin 원본을 보고 별도 이관한다.

## 작업 전 확인

- 현재 Kotlin 원본은 `bee303584cfaf9d0ba71668df79687de1f2122f8`에서 참조 가능하다.
- 다만 현재 작성한 plan 문서들은 아직 커밋되지 않은 상태일 가능성이 크다.
- 문서까지 기준점에 포함하려면 사용자가 먼저 커밋하는 것이 좋다.
- Git commit과 push는 사용자가 직접 진행한다.

## 실제 변경 계획

1. `.agents/CALENDAR.md` 상태를 변경한다.
   - `PLAN:Kotlin Java21 변환작업(...)`
   - `TASK:Kotlin Java21 변환작업(...)`
2. `build.gradle.kts`를 변경한다.
   - Kotlin 플러그인을 제거한다.
   - ktlint를 제거한다.
   - Java 플러그인을 명시한다.
   - Java toolchain을 17에서 21로 변경한다.
   - Kotlin 전용 의존성을 제거한다.
   - 기존 Spring, JPA, Security, Redis, Elasticsearch, PostgreSQL, Lombok, Testcontainers 의존성은 유지한다.
   - `kotlin { ... }` 설정을 제거한다.
   - `allOpen { ... }` 설정을 제거한다.
3. Kotlin 소스를 삭제한다.
   - `src/main/kotlin`
   - `src/test/kotlin`
4. Java 기본 구조를 생성한다.
   - `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java`
5. 테스트 기본 구조는 일단 보류한다.
   - 현재 resources가 datasource, Redis, Elasticsearch 환경변수에 의존한다.
   - context load 테스트가 바로 실패할 수 있다.
   - Step 1에서는 `compileJava` 가능한 상태를 우선 목표로 잡는다.
6. resources는 유지한다.
   - `application.yml`
   - profile yml
   - SQL 파일
   - 라이브러리 구성과 향후 Step 2 이관을 위한 설정 자산이므로 삭제하지 않는다.
   - 단, 환경변수가 없으면 부팅은 실패할 수 있으므로 Step 1 완료 기준은 컴파일 가능 상태로 둔다.
7. 검증을 진행한다.
   - 구현 후 `./gradlew compileJava` 실행을 후보로 둔다.
   - 테스트 실행은 하지 않는다.
   - 컴파일 확인도 명령 실행이므로 진행 전 별도 허락을 받는다.

## 예상 결과

- 프로젝트는 Java 21 Spring Boot 빈 프로젝트가 된다.
- Kotlin 코드는 워킹트리에서 사라진다.
- 기존 API 기능은 사라진다.
- 나중에 Step 2에서 기준 커밋의 Kotlin 원본을 보고 Java로 이관할 수 있다.

## 추천 결정

- Kotlin 소스는 삭제한다.
- resources는 유지한다.
- 테스트 클래스는 만들지 않는다.
- 완료 검증은 `./gradlew compileJava`로 한다.
- 문서 변경사항은 Step 1 시작 전에 사용자가 커밋하면 가장 안전하다.

## 진행 전 판단

- 커밋 없이 바로 Step 1을 진행해도 Kotlin 원본 자체는 HEAD에 있어서 복원 가능하다.
- 다만 지금 계획 문서까지 기준점으로 남기려면 커밋 후 시작하는 편이 낫다.
