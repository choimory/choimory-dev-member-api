# 목차

- [개요](#개요)
- [Java 빈 프로젝트 오류 확인](#java-빈-프로젝트-오류-확인)
- [메인 클래스 인식 문제 분석](#메인-클래스-인식-문제-분석)
- [Unsupported class file major version 70 분석](#unsupported-class-file-major-version-70-분석)

---

# 개요

- Kotlin에서 Java 21 기반 빈 Spring Boot 프로젝트로 전환한 뒤, IntelliJ IDEA에서 실행 설정이 정상 동작하지 않는 문제를 확인했다.
- 프로젝트는 기존 Kotlin 도메인 코드를 이관하는 목적이 아니라 Java로 바꾼 뒤 처음부터 다시 개발하기 위한 빈 프로젝트 상태다.
- 주요 증상은 Spring Boot Run Configuration에서 메인 클래스를 찾지 못하는 문제와 `Unsupported class file major version 70` 오류다.
- 코드 자체의 컴파일 문제인지, IDE/Gradle/JDK 설정 문제인지 분리해서 확인했다.

---

# Java 빈 프로젝트 오류 확인

> 자바로 컨버전했는데 오류난다 봐줘

## 답변

초기 확인 결과, 현재 프로젝트는 Java 소스가 `ChoimoryDevMemberApiApplication.java` 하나만 존재하는 빈 Spring Boot 프로젝트 상태였다.

`README.md`와 `.agents/histories/2026/08/08/kotlin-to-java21-migration/task1.md` 기록상 Kotlin 소스는 의도적으로 제거되었고, 기존 API 기능 복원은 후속 작업으로 분리된 상태였다.

## 확인 내용

- `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java`만 존재했다.
- `src/main/kotlin`, `src/test/kotlin`은 존재하지 않았다.
- Gradle 설정은 Groovy DSL 기반 Java 21 프로젝트로 구성되어 있었다.
- `./gradlew compileJava` 실행 전에는 Gradle wrapper가 사용자 홈의 `.gradle`에 접근해야 해서 권한 승인이 필요했다.

## 부팅 오류 후보

`application.yml`에서 `datasource` profile을 항상 include하고 있었다.

```yaml
spring:
    profiles:
        include: datasource
```

따라서 빈 프로젝트여도 부팅 시 PostgreSQL, Redis, Elasticsearch 설정값을 요구할 수 있는 상태였다.

---

# 메인 클래스 인식 문제 분석

> .agents/img.png봐봐 메인 메소드가 안잡혀

## 답변

스크린샷의 오류는 메인 메소드가 없다는 뜻이 아니라, IntelliJ Run Configuration이 지정한 클래스를 현재 모듈 클래스패스에서 찾지 못한다는 의미로 판단했다.

## 근거

- Java 파일에는 정상적인 `main` 메소드가 존재했다.
- Gradle 컴파일 후 클래스 파일도 생성되어 있었다.
- `build/classes/java/main/dev/choimory/member/api/ChoimoryDevMemberApiApplication.class`가 존재했다.

## 원인 후보

`.idea/choimory-dev-member-api.iml`에 source root가 잡혀 있지 않았다.

```xml
<content url="file://$MODULE_DIR$" />
```

이 상태에서는 IntelliJ가 `src/main/java`를 Java source root로 인식하지 못해 Run Configuration에서 클래스를 찾지 못할 수 있다.

---

# Unsupported class file major version 70 분석

> Unsupported class file major version 70
 이오류는 뭐지

## 답변

`Unsupported class file major version 70`은 Java 26으로 컴파일된 `.class` 파일을 현재 실행하거나 분석하는 도구가 읽지 못한다는 의미다.

## Java class major version

```text
Java 21 -> major version 65
Java 22 -> major version 66
Java 23 -> major version 67
Java 24 -> major version 68
Java 25 -> major version 69
Java 26 -> major version 70
```

## 확인된 설정 불일치

IntelliJ IDEA 설정에 여러 JDK 버전이 섞여 있었다.

- Gradle JVM: `24 (2)`
- Project language level: `JDK_17`
- bytecode target: `17`
- 모듈 JDK: `24 (2)`
- 프로젝트 Gradle toolchain: Java 21
- 로컬 설치 JDK: Java 26, Java 21, Java 17

## 계획

- IntelliJ 프로젝트 설정을 Java 21 기준으로 통일한다.
- `src/main/java`, `src/main/resources`를 source/resource root로 명시한다.
- Kotlin 설정 잔재를 제거한다.
- Run Configuration의 모듈명을 실제 등록 모듈명과 맞춘다.
- Gradle 컴파일 결과가 Java 21 class major version 65인지 확인한다.
