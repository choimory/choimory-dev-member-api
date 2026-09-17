# 목차

- [개요](#개요)
- [Kotlin 프로젝트 Java 21 전환 분석](#kotlin-프로젝트-java-21-전환-분석)
- [Java 21 빈 프로젝트와 라이브러리 이식 방향](#java-21-빈-프로젝트와-라이브러리-이식-방향)
- [Step 1, Step 2 분리 진행 방향](#step-1-step-2-분리-진행-방향)
- [Step 2 계획 선작성 방향](#step-2-계획-선작성-방향)
- [Step 1 이후 Step 2 원본 참조 방식](#step-1-이후-step-2-원본-참조-방식)
- [Git 히스토리 참조 가능 여부 확인](#git-히스토리-참조-가능-여부-확인)

---

# 개요

- 현재 프로젝트를 Java 21로 전환

---

# Kotlin 프로젝트 Java 21 전환 분석

> 분석해봐

## 답변

코드/문서 수정, 테스트 실행 없이 현재 프로젝트의 Java 21 전환 범위를 분석했다.

## 현재 상태

- Kotlin 소스는 총 41개 파일, 약 871라인이다.
- `src/main/kotlin`에는 38개 파일이 있다.
- `src/test/kotlin`에는 3개 파일이 있다.
- 빌드는 `build.gradle.kts` 기반이다.
- 현재 Kotlin JVM/Spring/JPA 플러그인을 사용한다.
- 현재 Java toolchain은 17로 설정되어 있다.
- 목표인 Java 21로 변경해야 한다.
- Gradle Wrapper는 8.14.2라 Java 21 빌드 자체는 문제 없을 가능성이 크다.

## 빌드 전환 대상

- Kotlin 플러그인 제거가 필요하다.
  - `kotlin("jvm")`
  - `kotlin("plugin.spring")`
  - `kotlin("plugin.jpa")`
- Kotlin 관련 의존성 제거가 필요하다.
  - `jackson-module-kotlin`
  - `kotlin-reflect`
  - `kotlin-logging-jvm`
  - `kotlin-test-junit5`
- `ktlint` 플러그인 제거 또는 Java formatter 대체 여부 결정이 필요하다.
- Java toolchain을 21로 변경해야 한다.

## 전환 난이도

- 전체 규모는 작아서 전체 Java 변환이 가능하다.
- 가장 반복 작업이 많은 곳은 `data class` 기반 DTO, Request, Response다.
- Kotlin 특화 문법은 주로 `data class`, `companion object`, `lateinit`, null-safe 연산, safe cast, Kotlin logging 정도다.
- 서비스, 컨트롤러, 설정 클래스는 Java로 거의 직역 가능하다.

## 주의 지점

### LoginRequest 패키지 불일치

- `src/main/kotlin/dev/choimory/member/api/login/v1/domain/request/LoginRequest.kt`의 파일 경로와 package가 다르다.
- 파일 위치는 `dev/choimory/member/api/...` 경로다.
- 실제 package는 `io.choimory.member.external.api.login.domain.request`다.
- `LoginProcessFilter.kt`도 해당 외부 패키지를 import 중이다.
- 의도된 외부 API 패키지인지, 단순 오타인지 먼저 결정해야 한다.

### Controller RequestBody 누락 가능성

- `MemberCommandController.kt`의 요청 파라미터에 `@RequestBody`가 없다.
- JSON API라면 Java 전환과 별개로 현재도 바인딩 문제가 날 수 있다.
- 기존 상태를 보존할지, Java 전환 중 같이 정상화할지 결정해야 한다.

### TODO 및 미구현 로직

- `MemberDocumentDto.from()`이 `TODO()` 상태다.
- 토큰 생성 로직도 TODO 상태다.
- 이메일 발송 Kafka도 TODO 상태다.
- 전환 작업에서는 미구현 로직을 그대로 Java로 옮길지, 함께 보완할지 범위를 정해야 한다.

### Redis 직렬화

- Redis에 `MemberEntityDto`를 JSON 직렬화하고 있다.
- Java DTO 변환 시 기본 생성자 또는 Jackson 생성자 정책을 신경 써야 한다.
- 불변 DTO로 작성할 경우 `@JsonCreator`, `@JsonProperty` 적용 여부를 검토해야 한다.

### JPA Entity

- Kotlin JPA 플러그인이 처리하던 open/no-arg 관련 부분을 Java에서는 직접 처리해야 한다.
- Java Entity에는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Getter`, 필요한 생성자 또는 정적 팩토리 적용을 검토해야 한다.
- 상속 구조와 JPA Auditing 필드는 유지해야 한다.

## 추천 진행 순서

1. 빌드 설정을 Java 21 기준으로 전환한다.
2. Kotlin 플러그인과 Kotlin 의존성을 제거한다.
3. `src/main/kotlin`을 `src/main/java`로 전환한다.
4. `src/test/kotlin`을 `src/test/java`로 전환한다.
5. Entity, Document, DTO, Request, Response를 먼저 변환한다.
6. 설정, 컨트롤러, 서비스, 핸들러를 변환한다.
7. 보안 필터와 인증 핸들러를 변환한다.
8. 컴파일 확인을 진행한다.
9. 테스트 실행은 별도 허락을 받고 진행한다.
10. 작업 마무리 시 README에 Kotlin에서 Java 21로 변경된 내용을 반영한다.

## 결정 필요 사항

- `LoginRequest` 패키지 불일치를 기존 상태로 보존할지, 프로젝트 패키지에 맞게 정상화할지 결정해야 한다.
- 누락된 `@RequestBody`를 기존 상태로 보존할지, Java 전환 중 같이 보완할지 결정해야 한다.
- TODO 로직을 그대로 옮길지, 전환 과정에서 같이 구현할지 결정해야 한다.
- DTO, Request, Response를 Lombok 기반 불변 클래스로 작성할지, Java record를 사용할지 결정해야 한다.

---

# Java 21 빈 프로젝트와 라이브러리 이식 방향

> 엔티티랑 dto랑 작성된거는 안옮기고 그냥 비어있는 프로젝트에 라이브러리만 똑같이 추가해놓는다고 하면?

## 답변

기존 Entity, DTO, Service, Controller 등 작성된 Kotlin 코드를 Java로 변환하지 않는다면 작업 범위가 훨씬 작아진다.

이 방향은 “Kotlin 코드를 Java로 변환”이 아니라 “Java 21 Spring Boot 빈 프로젝트로 재구성하고, 기존 라이브러리 구성만 이식”하는 작업에 가깝다.

## 작업 범위

1. `src/main/kotlin`, `src/test/kotlin` 소스는 Java로 변환하지 않는다.
2. Java 기본 디렉토리만 구성한다.
   - `src/main/java`
   - `src/test/java`
3. 최소 실행 클래스만 Java로 생성한다.
   - `ChoimoryDevMemberApiApplication.java`
4. Gradle 설정을 Java 21 기준으로 변경한다.
5. 기존 기능 라이브러리 구성은 유지한다.
6. Kotlin 전용 플러그인과 의존성은 제거한다.
7. README는 작업 마무리 시 Java 21 기준으로 반영한다.

## 유지할 라이브러리

- Spring Web
- Spring Security
- Spring Data JPA
- Spring Data Elasticsearch
- Elasticsearch Java API Client
- Spring Data Redis
- Spring Validation
- PostgreSQL Driver
- Lombok
- Jackson Databind
- UUID Creator
- Testcontainers
- JUnit5
- Spring Security Test

## 제거할 Kotlin 전용 구성

- Kotlin Gradle 플러그인
  - `kotlin("jvm")`
  - `kotlin("plugin.spring")`
  - `kotlin("plugin.jpa")`
- Ktlint 플러그인
  - `org.jlleitschuh.gradle.ktlint`
- Kotlin 전용 의존성
  - `jackson-module-kotlin`
  - `kotlin-reflect`
  - `kotlin-logging-jvm`
  - `kotlin-test-junit5`
- Kotlin 컴파일 설정
  - `kotlin { compilerOptions { ... } }`
- Kotlin JPA open 설정
  - `allOpen { ... }`

## 주의점

- 기존 Entity, DTO, Service, Controller를 옮기지 않으면 현재 API 기능은 사라진다.
- 결과물은 기존 기능을 가진 Java 프로젝트가 아니라, 기존 의존성 구성을 참고한 Java 21 빈 백엔드 프로젝트가 된다.
- `application.yml`, SQL 파일을 유지할 경우 DB, Redis, Elasticsearch 설정 때문에 실행 시 환경변수 누락으로 부팅 실패할 수 있다.
- 라이브러리만 맞춰둔 빈 프로젝트라면 설정 파일 유지 여부를 별도로 결정해야 한다.

## 방향 정리

이 방향의 작업명은 “Kotlin 프로젝트 Java 21 변환”보다는 “Java 21 베이스 프로젝트 초기화”가 더 정확하다.

목표를 다음처럼 정리할 수 있다.

- 기존 도메인 코드는 이관하지 않는다.
- Java 21 기반 빈 Spring Boot 프로젝트를 구성한다.
- 기존 프로젝트에서 사용하던 주요 기능 라이브러리만 Java 프로젝트에 맞게 재구성한다.

---

# Step 1, Step 2 분리 진행 방향

> 그럼 스텝1, 2로 바꿔서 1로 초기화만 하고 로직 이관은 스텝2로 한다면?

## 답변

작업을 “한 번에 Kotlin에서 Java로 전체 변환”으로 잡지 않고, Step 1과 Step 2로 나누는 방향이 가장 깔끔하다.

Step 1에서는 Java 21 베이스 프로젝트 초기화만 진행하고, Step 2에서 기존 Kotlin 로직을 Java로 이관한다.

## Step 1. Java 21 베이스 프로젝트 초기화

- Kotlin 소스는 이관하지 않는다.
- 기존 기능 로직도 이관하지 않는다.
- Java 21 Spring Boot 프로젝트로 빌드 구조만 정리한다.
- 기존에 쓰던 주요 라이브러리만 Java 기준으로 재구성한다.
- Kotlin 전용 플러그인과 의존성을 제거한다.
- 최소 실행 클래스만 Java로 생성한다.
- 필요하면 빈 테스트 클래스 정도만 생성한다.
- 목표는 Java 21 프로젝트가 컴파일 또는 부팅 가능한 상태가 되는 것이다.

## Step 2. 기존 Kotlin 로직 Java 이관

- Step 1 완료 후 별도 작업으로 진행한다.
- Entity, DTO, Request, Response를 Java로 이관한다.
- Repository를 Java로 이관한다.
- Service, Handler, Controller를 Java로 이관한다.
- Security Filter, Provider, Handler를 Java로 이관한다.
- Redis, Elasticsearch, JPA 세부 동작을 검증한다.
- 기존 TODO, 패키지 불일치, `@RequestBody` 누락 같은 문제를 이 단계에서 처리할지 결정한다.
- 목표는 기존 API 기능을 Java 코드로 복원하는 것이다.

## 계획 정리

- CALENDAR의 `Kotlin Java21 변환작업`은 전체 상위 작업명으로 유지할 수 있다.
- 실제 구현은 Step 1부터 시작한다.
- Step 1 작업명은 “Java 21 베이스 프로젝트 초기화”로 잡는 것이 명확하다.
- Step 2 작업명은 “기존 Kotlin 로직 Java 이관”으로 잡는 것이 명확하다.
- 이렇게 나누면 Step 1은 낮은 리스크로 빠르게 끝낼 수 있다.
- Step 2는 도메인별로 쪼개서 이관하고, 중간중간 컴파일 확인하기 쉽다.

---

# Step 2 계획 선작성 방향

> 그럼 step1하고 나중에 step2를 바로 진행하는게 아니라 나중에 시간될때 따로 할 수 있게 구현계획이나 이런것들 다 작성해놓을수 있나 미리?

## 답변

가능하다.

Step 1을 구현하기 전에 Step 2를 나중에 바로 이어갈 수 있도록 “이관 계획서”를 미리 상세히 작성해둘 수 있다.

## 문서 구성

- `plan1.md`에는 전체 방향, Step 1과 Step 2 분리 이유, 작업 범위, 결정 사항, 보류 사항을 정리한다.
- `plan2.md`에는 Step 1 상세 구현 계획을 작성한다.
- `plan3.md`에는 Step 2 상세 이관 계획을 작성한다.

## plan2.md 작성 내용

- Step 1 상세 구현 계획
- 어떤 파일을 삭제, 이동, 생성할지
- `build.gradle.kts` 변경 계획
- 유지할 의존성 목록
- 제거할 의존성 목록
- 최소 Java Application 클래스 구성
- Step 1 완료 기준

## plan3.md 작성 내용

- Step 2 상세 이관 계획
- 도메인별 이관 순서
- Kotlin 파일에서 Java 파일로의 매핑표
- Entity, DTO, Request, Response 변환 규칙
- Controller, Service, Handler 변환 규칙
- Security 변환 규칙
- 기존 TODO 처리 정책
- 검증 순서
- 예상 리스크

## 중요 포인트

- Step 2는 나중에 시간이 지나도 바로 시작할 수 있게 매핑표가 중요하다.
- “나중에 결정할 것”도 문서에 분리해서 적어두는 것이 좋다.
- Step 1은 지금 바로 실행 가능한 작업 계획이 된다.
- Step 2는 나중에 컨텍스트 없이도 이어가기 쉬운 백로그가 된다.

## 나중에 결정할 것

- `LoginRequest` 패키지 불일치 수정 여부
- `@RequestBody` 추가 여부
- TODO 구현 여부
- DTO를 Lombok class로 할지 Java record로 할지
- 기존 Kotlin 소스를 Step 1에서 삭제할지, 보관 디렉토리로 옮길지
- Step 2를 도메인별로 할지, 레이어별로 할지

---

# Step 1 이후 Step 2 원본 참조 방식

> step1 진행하고 나면 예전코드가 사라져 있을텐데 그래도 나중에 step2 진행하는게 가능할까?

## 답변

가능하지만 조건이 있다.

Step 1에서 Kotlin 소스를 삭제하면 나중에 Step 2는 현재 워킹트리만 보고 진행하기 어렵고, Git 히스토리에서 예전 Kotlin 코드를 꺼내와야 한다.

## 검토한 선택지

### 1. Kotlin 소스를 삭제하지 않고 빌드에서만 제외

- 가장 안전한 방식이다.
- `src/main/kotlin`, `src/test/kotlin`은 그대로 남겨둔다.
- Gradle에서 Kotlin 플러그인을 제거하면 기본 Java 빌드에서는 Kotlin 소스를 컴파일하지 않는다.
- 나중에 Step 2 때 기존 Kotlin 파일을 바로 참고할 수 있다.
- 단점은 Java 21 빈 프로젝트처럼 보이지 않고 예전 코드가 남아 있다는 점이다.
- 사용자가 원하지 않는 방식이므로 제외한다.

### 2. Kotlin 소스를 보관 디렉토리로 이동

- 예: `.agents/2026/08/08/kotlin-to-java21-migration/legacy-kotlin-source/`
- 나중에 Step 2 때 바로 참고할 수 있다.
- 단점은 소스 전체를 문서 히스토리 영역에 복사 또는 이동하게 되어 저장소가 지저분해질 수 있다.
- `.agents`가 작업 히스토리 역할을 하긴 하지만, 코드 원본 전체 보관 장소로 쓰기에는 무겁다.

### 3. Kotlin 소스를 삭제하고 Git 히스토리에 의존

- Step 1 결과가 가장 깔끔하다.
- 나중에 Step 2 때 `git show`, `git checkout <commit> -- <path>` 같은 방식으로 예전 파일을 참고한다.
- 단점은 기준 커밋 또는 브랜치가 명확하지 않으면 나중에 복원이 번거롭다.
- 커밋은 사용자가 직접 한다는 규칙이 있으므로 Step 1 전후 커밋 관리를 사용자가 직접 해야 한다.

## 결정

> 그럼 3번으로 하자 이것도 plan 추가

Step 1에서는 3번 방식을 사용한다.

- Step 1 전에 사용자가 현재 Kotlin 코드 상태를 직접 커밋한다.
- Step 1에서는 Kotlin 소스를 삭제해서 Java 21 빈 프로젝트로 정리한다.
- Step 2에서는 Step 1 이전 커밋의 Kotlin 원본을 Git 히스토리에서 참조한다.
- Step 2 문서에는 “기존 Kotlin 원본은 Step 1 이전 커밋에서 참조한다”는 내용을 명시한다.

## Step 1 전제 조건

- Step 1 작업 시작 전 사용자가 현재 상태를 커밋해야 한다.
- 커밋 해시는 Step 2에서 Kotlin 원본 참조 기준점으로 사용한다.
- 사용자가 커밋하지 않은 상태에서 Step 1을 진행하면 Kotlin 원본 복원이 어려워질 수 있다.

---

# Git 히스토리 참조 가능 여부 확인

> git 확인해서 3번 진행하는데 무리 없을지 미리 봐두는것도 좋겠다

## 답변

Git 상태를 확인한 결과 3번 방식으로 진행해도 무리 없다.

## 확인 결과

- 현재 브랜치는 `feature/conversion-to-java`다.
- 현재 HEAD는 `bee303584cfaf9d0ba71668df79687de1f2122f8`다.
- Kotlin 파일 41개가 모두 Git에 추적 중이다.
- Kotlin 소스 쪽 uncommitted 변경은 없다.
- 현재 변경사항은 문서 작업뿐이다.
  - `.agents/CALENDAR.md` 수정
  - `.agents/2026/08/` 신규 문서들

## 원본 조회 확인

- `git show HEAD:src/main/kotlin/...` 방식으로 Kotlin 원본 조회가 가능한지 확인했다.
- `ChoimoryDevMemberApiApplication.kt` 원본 조회가 가능했다.
- `MemberEntity.kt` 원본 조회가 가능했다.
- 따라서 Step 1에서 `src/main/kotlin`, `src/test/kotlin`을 삭제하더라도 나중에 Step 2에서 현재 HEAD 기준으로 Kotlin 원본을 참조할 수 있다.

## 주의 사항

- Kotlin 원본만 기준으로 보면 현재 HEAD인 `bee303584cfaf9d0ba71668df79687de1f2122f8`에 이미 포함되어 있다.
- 다만 지금 작성한 plan 문서들까지 Step 2 기준점에 포함하려면 Step 1 시작 전에 사용자가 현재 문서 변경사항을 커밋해두는 것이 좋다.
- Git commit과 push는 사용자가 직접 진행한다.
