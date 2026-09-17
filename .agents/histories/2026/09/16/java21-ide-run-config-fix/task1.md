# 목차

- [개요](#개요)
- [IDE 설정 수정](#ide-설정-수정)
- [Run Configuration 수정](#run-configuration-수정)
- [검증](#검증)

---

# 개요

- IntelliJ IDEA가 Java 21 빈 Spring Boot 프로젝트의 메인 클래스를 찾지 못하는 문제를 수정했다.
- 원인은 Java 코드가 아니라 `.idea` 프로젝트 설정의 JDK 버전, source root, 모듈명 불일치였다.
- 프로젝트 소스 코드는 변경하지 않고 IDE 메타데이터만 정리했다.

---

# IDE 설정 수정

> 문제사항들 좀 수정해줘 안된다 맞춰봐도

## 답변

IntelliJ 설정이 서로 충돌하는 상태였기 때문에 Java 21 기준으로 통일했다.

## 수정 파일

### `.idea/choimory-dev-member-api.iml`

- `src/main/java`를 source root로 등록했다.
- `src/main/resources`를 resource root로 등록했다.
- `.gradle`, `build` 디렉토리를 제외 처리했다.
- 모듈 JDK를 `24 (2)`에서 `jbr-21.0.7`로 변경했다.

### `.idea/misc.xml`

- Project language level을 `JDK_17`에서 `JDK_21`로 변경했다.
- Project JDK를 `17`에서 `jbr-21.0.7`로 변경했다.

### `.idea/compiler.xml`

- bytecode target을 `17`에서 `21`로 변경했다.
- annotation processing profile의 모듈명을 실제 모듈명에 맞게 변경했다.

### `.idea/gradle.xml`

- Gradle JVM을 `24 (2)`에서 `jbr-21.0.7`로 변경했다.

### `.idea/kotlinc.xml`

- Java 프로젝트에 남아 있던 Kotlin compiler 설정 잔재라 삭제했다.

---

# Run Configuration 수정

## 문제

`.idea/workspace.xml`의 Spring Boot Run Configuration이 존재하지 않는 모듈명을 참조하고 있었다.

```xml
<module name="choimory-dev-member-api.main" />
```

실제 `.idea/modules.xml`에 등록된 모듈은 `choimory-dev-member-api`였다.

## 수정

Run Configuration의 모듈명을 실제 등록 모듈명으로 변경했다.

```xml
<module name="choimory-dev-member-api" />
```

메인 클래스 값은 유지했다.

```xml
<option name="SPRING_BOOT_MAIN_CLASS" value="dev.choimory.member.api.ChoimoryDevMemberApiApplication" />
```

---

# 검증

## Gradle 컴파일

`./gradlew compileJava`를 실행해 Java 컴파일이 정상인지 확인했다.

```text
BUILD SUCCESSFUL
```

## 클래스 major version 확인

`javap -verbose`로 생성된 클래스 파일의 major version을 확인했다.

```text
minor version: 0
major version: 65
```

`major version: 65`는 Java 21을 의미한다.

## 잔여 설정 확인

`.idea` 아래에 다음 잔재가 남아 있지 않은지 확인했다.

- `JDK_17`
- `jdkName="24`
- `gradleJvm`의 Java 24 참조
- bytecode target 17
- Kotlin compiler 설정
- `src/main/kotlin`
- 존재하지 않는 `choimory-dev-member-api.main` 모듈명

검색 결과 남은 충돌 설정은 없었다.
