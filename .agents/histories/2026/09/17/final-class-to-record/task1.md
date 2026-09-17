# 목차

- [개요](#개요)
- [구현 시작](#구현-시작)
- [컴파일 오류 확인](#컴파일-오류-확인)

---

# 개요

- `final class` 기반 데이터 객체들을 Java record로 전환하는 작업을 진행했다.
- record 전환 방식은 `record + @Builder(toBuilder = true)`로 통일했다.
- Entity, Elasticsearch root Document, Exception, Util 클래스는 record 전환 대상에서 제외했다.
- 구현 후 `./gradlew compileJava`를 실행해 컴파일 성공까지 확인했다.

---

# 구현 시작

> 구현 시작

## 답변

구현 시작 전 다음 계획을 설명했다.

1. `calendar.md`의 해당 작업 상태를 `PLAN`에서 `TASK`로 변경한다.
2. `plan1.md` 기준으로 record 전환 대상을 확정한다.
3. 데이터 객체들을 `record + @Builder(toBuilder = true)`로 변경한다.
4. 기존 getter 호출부를 record accessor로 변경한다.
   - `getEmail()` → `email()`
   - `getVerifyCode()` → `verifyCode()`
5. 정적 팩토리 메소드는 유지하고 내부 생성은 builder를 사용한다.
6. Entity, Exception, Util, Spring Bean은 record 전환에서 제외한다.
7. 컴파일/테스트는 별도 허락을 받고 실행한다.

---

> 시작

## 답변

사용자 확인 후 구현을 시작했다.

## calendar 상태 변경

`.agents/histories/calendar.md`에서 `final-class-to-record` 작업 상태를 `PLAN`에서 `TASK`로 변경했다.

## record 전환 대상

다음 데이터 객체들을 `record + @Builder(toBuilder = true)` 형태로 전환했다.

- `CommonDateDocument`
- `CommonValidateResult`
- `CommonResponse<T>`
- `LoginRequest`
- `LoginResponse`
- `TokenDetails`
- `MemberEntityDto`
- `MemberDocumentDto`
- `CreateMemberRequest`
- `VerifyMemberRequest`
- `CreateMemberResponse`
- `VerifyMemberResponse`
- `FindMemberRequest`
- `FindMemberResponse`

## 제외 대상

다음 객체들은 record 전환 대상에서 제외했다.

- `TimeUnitUtil`
  - 상태를 가지는 데이터 객체가 아니라 유틸리티 클래스다.
- Entity
  - JPA 기본 생성자, 프록시, 영속성 컨텍스트 제약이 있다.
- Elasticsearch root Document
  - Spring Data Elasticsearch 매핑 대상이다.
- Exception
  - 예외 계층과 상속 구조가 필요하다.

## 호출부 변경

record로 전환된 객체를 사용하는 호출부는 getter 방식에서 record accessor 방식으로 변경했다.

- `LoginProcessFilter`
  - `loginRequest.getEmail()` → `loginRequest.email()`
  - `loginRequest.getPassword()` → `loginRequest.password()`
- `MemberCommandService`
  - `request.getPassword()` → `request.password()`
  - `request.getEmail()` → `request.email()`
  - `request.getNickname()` → `request.nickname()`
  - `request.getIntroduce()` → `request.introduce()`
  - `payload.getUuid()` → `payload.uuid()`
  - `payload.getEmail()` → `payload.email()`
  - `payload.getVerifyCode()` → `payload.verifyCode()`
- `MemberCommandHandler`
  - `member.getId()` → `member.id()`
  - `memberEntityDto.getEmail()` → `memberEntityDto.email()`
- `TokenDetails`
  - `member.getEmail()` → `member.email()`
- `MemberEntityDto`
  - DTO 내부 변환에서 `dto.get...()` 호출을 record accessor로 변경했다.

## 정적 확인

`git diff --check`를 실행해 공백 오류가 없음을 확인했다.

남은 `final class`는 제외 대상인 `TimeUnitUtil`만 확인했다.

---

# 컴파일 오류 확인

> 컴파일오류있어 확인해봐

## 답변

`./gradlew compileJava`를 실행해 컴파일 오류를 확인했다.

## 오류 내용

`MemberQueryHandler`에서 `MemberDocument`를 record로 착각하고 다음처럼 변경한 부분에서 컴파일 오류가 발생했다.

```java
member.password()
```

오류 메시지는 다음 내용이었다.

```text
cannot find symbol
symbol: method password()
location: variable member of type MemberDocument
```

`MemberDocument`는 record 전환 제외 대상인 Elasticsearch root Document이므로 Lombok getter를 유지해야 한다.

## 수정 내용

`MemberQueryHandler`의 비밀번호 검증 코드를 다시 getter 호출로 수정했다.

```java
member.getPassword()
```

## 검증 결과

수정 후 `./gradlew compileJava`를 다시 실행했고 성공했다.

```text
BUILD SUCCESSFUL
```

이번 단계에서는 `compileTestJava`와 `test`는 실행하지 않았다.
