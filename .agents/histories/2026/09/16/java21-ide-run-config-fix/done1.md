# 목차

- [개요](#개요)
- [완료 내용](#완료-내용)
- [검증 결과](#검증-결과)
- [후속 참고](#후속-참고)

---

# 개요

- Java 21 빈 Spring Boot 프로젝트에서 IntelliJ IDEA가 메인 클래스를 찾지 못하고, `Unsupported class file major version 70` 오류가 발생하던 문제를 정리했다.
- 프로젝트 소스 코드 문제는 아니었고, IntelliJ IDEA의 프로젝트 메타데이터에 JDK 17, JDK 24, Java 21 설정이 섞여 있던 것이 주요 원인이었다.

---

# 완료 내용

> 잘됐다

## 답변

IDE 설정을 Java 21 기준으로 정리한 뒤 정상 동작을 확인했다.

## 변경 요약

- `src/main/java`를 source root로 등록했다.
- `src/main/resources`를 resource root로 등록했다.
- Project language level을 Java 21로 변경했다.
- Gradle JVM을 Java 21 JBR로 변경했다.
- bytecode target을 21로 변경했다.
- 모듈 JDK를 Java 21 JBR로 변경했다.
- Java 프로젝트에 남아 있던 Kotlin compiler 설정 파일을 삭제했다.
- Spring Boot Run Configuration의 모듈명을 실제 등록 모듈명과 일치시켰다.

## 수정 파일

- `.idea/choimory-dev-member-api.iml`
- `.idea/misc.xml`
- `.idea/compiler.xml`
- `.idea/gradle.xml`
- `.idea/workspace.xml`
- `.idea/kotlinc.xml`

---

# 검증 결과

## 컴파일

```text
./gradlew compileJava
BUILD SUCCESSFUL
```

## 클래스 버전

```text
major version: 65
```

Java class major version 65는 Java 21에 해당한다.

## 최종 판단

- Java 소스의 메인 메소드는 정상이다.
- Gradle 컴파일도 정상이다.
- `Unsupported class file major version 70` 문제는 Java 26 산출물 또는 캐시를 낮은 버전의 도구가 읽으려던 상황으로 판단했다.
- IDE 프로젝트 설정을 Java 21 기준으로 맞춰 문제를 해결했다.

---

# 후속 참고

- IntelliJ에서 비슷한 문제가 다시 발생하면 source root, Run Configuration module, Project SDK, Gradle JVM을 먼저 확인한다.
- Gradle Reload 후에도 빨간 줄이 남으면 IntelliJ cache invalidate가 필요할 수 있다.
- 현재 프로젝트는 빈 Java 21 Spring Boot 베이스 프로젝트이며, 기존 Kotlin 도메인 기능은 이관되지 않은 상태가 맞다.
