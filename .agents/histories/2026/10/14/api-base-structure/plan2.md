# 목차

- [개요](#개요)
- [기존 코드(8754490d) 참고](#기존-코드8754490d-참고)
- [추가1](#추가1)
- [범위 확정과 패키지 위치](#범위-확정과-패키지-위치)
- [작업 계획](#작업-계획)

---

# 개요

- member-api에 공용 예외 핸들러 추가
- member-domain에 공용 예외, 공용 요청, 공용 페이지 요청, 공용 응답객체 생성

---

# 기존 코드(8754490d) 참고

> git log 8754490d보면 예전에 만들었던거 있는데 이거 보면 대충 알수 있음

## 답변

- 커밋 `8754490d`(refactor: 기존 코틀린 코드 추가)에 공용 응답, 공용 예외, 예외 핸들러, 검증 실패 객체가 있다.
- 페이지 요청과 페이지 응답은 기존 코드에 없어서 새로 설계해야 한다.

## 기존 코드 요약

| 클래스 | 내용 |
|---|---|
| `CommonResponse<T>` | `code`, `name`, `message`, `data`를 가지는 불변 응답 래퍼. `@Builder(toBuilder = true)`와 `@JsonCreator`를 쓰고, 데이터 없는 생성자도 있음 |
| `CommonValidateResult` | 검증 실패 정보(`field`, `rejectValue`, `message`) |
| `CommonException` | `RuntimeException`을 상속하고 `HttpStatus`, `code`, `codeName`, `msg`를 가짐 |
| `CommonExceptionHandler` | `@RestControllerAdvice("dev.choimory")`. `Exception`, `RuntimeException`, `CommonException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `AuthenticationException`을 처리 |

## 새 구조에 옮길 때 확인할 점

### HttpStatus 의존성

- 기존 `CommonException`은 Spring의 `HttpStatus`를 쓴다.
- `CommonException`을 member-domain에 두면 `spring-web` 의존성이 필요하다.
- 대안은 `int status`나 직접 만든 enum을 쓰고, member-api의 핸들러가 `HttpStatus`로 변환하는 것이다.

### member-domain 의존성

- 현재 member-domain에는 `spring-boot-starter-data-jpa`와 Lombok만 있다.
- `CommonResponse`와 `CommonValidateResult`는 Jackson 어노테이션을 쓰고, 검증 어노테이션을 쓰는 요청 객체가 있다면 validation도 필요하다.
- 기존에는 이 클래스들이 member-api 안에 있었다. 추가할 의존성은 plan2에서 정한다.

### Exception과 RuntimeException 핸들러 중복

- 둘 다 500을 반환하고, `e.getMessage()`를 응답 `data`로 그대로 내보낸다.
- 내부 메시지가 노출되는 문제가 있어서, `RuntimeException` 핸들러는 없애고 `data`에 메시지를 넣지 않는 쪽을 제안한다.

### AuthenticationException 핸들러

- 시큐리티 필터에서 던지는 예외는 `@RestControllerAdvice`에 잡히지 않는 경우가 많다.
- 이번에 포함할지, 로그인과 시큐리티 작업으로 미룰지 정해야 한다.

### 공용 요청과 페이지 요청

- 기존 코드에 없어서 새로 설계해야 한다.
- 공용 요청이 공통 부모 클래스인지, 요청 객체의 공통 규칙인지 정해야 한다.
- 페이지 요청은 `page`와 `size`에 정렬까지 넣을지 정해야 한다.
- 페이지 응답(목록, 총 개수 등)을 이번 범위에 넣을지 정해야 한다.

---

# 추가1

- 공통 요청은 필요없겠다
- 공통 페이징 요청도 지금은 제외
- 공통 예외는 member-api로 옮기자 
- 예외 핸들러는 일딴 저대로 만들어놓고 내가 수정할거 봐줄게 

---

# 범위 확정과 패키지 위치

> 2,3번 그렇게 하고. 1번은 일단 예전코드대로 만들고 추가수정할거임

## 답변

### 범위

- 추가1의 결정에 따라 공통 요청과 공통 페이징 요청은 이번 범위에서 제외한다.
- 공용 예외(`CommonException`)와 예외 핸들러(`CommonExceptionHandler`)는 member-api에 둔다.
- 공용 응답(`CommonResponse`)과 검증 실패 객체(`CommonValidateResult`)는 member-domain에 둔다.
- `HttpStatus`와 `AuthenticationException`은 member-api 안에서 쓰므로 member-domain에 `spring-web` 의존성은 필요 없다. 응답 객체의 Jackson 어노테이션 때문에 Jackson 의존성만 member-domain에 추가한다.

### 예외 핸들러

- 예전 코드(8754490d)의 `CommonExceptionHandler`를 일단 그대로 옮긴다.
- 다음 핸들러를 포함한다: `Exception`, `RuntimeException`, `CommonException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `AuthenticationException`
- `RuntimeException` 핸들러 중복과 500 응답의 `e.getMessage()` 노출은 그대로 두고, 추후 수정한다.

### 패키지 위치

- member-api: `dev.choimory.member.api.common.exception` (`CommonException`, `CommonExceptionHandler`)
- member-domain: 기존 `common` 하위에 둔다. (`CommonResponse`, `CommonValidateResult`)
  - 하위 패키지는 기존 `common/command/entity` 구조에 맞춰 `common/command/{response,dto}`를 제안하며, 구현 시작 전에 확인한다.

---

# 작업 계획

## 변경 사항 (기존 계획 대비)

- **데이터 객체는 record로 작성한다.**
  - 프로젝트가 `record + @Builder(toBuilder = true)` 기준으로 정리되어 있으므로, 기존 코드의 final class `CommonResponse`, `CommonValidateResult`는 record로 옮긴다.
  - record는 component 이름과 JSON 필드명이 같아 `@JsonProperty`와 `@JsonCreator`가 필요 없다.
  - 따라서 member-domain에 Jackson 의존성을 추가하지 않는다. (앞 섹션의 "Jackson 의존성 추가"를 대체한다)
  - `CommonException`은 record 전환 제외 대상(Exception)이므로 class로 둔다.
- **member-domain 패키지는 `common/command/response`로 한다.**
  - `CommonResponse`와 `CommonValidateResult`를 모두 이곳에 둔다.
  - `command/dto` 패키지는 Entity와 1:1로 매칭되는 DTO 전용이고 `DtoRulesTest`가 이 패키지를 검사하므로, 응답 값 객체는 `response`에 둔다.

## 생성 대상

| 모듈 | 패키지 | 클래스 |
|---|---|---|
| member-domain | `dev.choimory.member.domain.common.command.response` | `CommonResponse<T>`(code, name, message, data / 데이터 없는 생성자), `CommonValidateResult`(field, rejectValue, message) |
| member-api | `dev.choimory.member.api.common.exception` | `CommonException`, `CommonExceptionHandler` |

- `CommonExceptionHandler`는 예전 코드(8754490d)를 그대로 옮긴다. (6개 예외 처리, `@RestControllerAdvice("dev.choimory")`)
- 규칙에 따라 클래스, 필드(record component), 함수에 Doc Comment를 작성하고, 함수 내부에는 흐름별 한글 주석을 작성한다.

## 진행 순서

1. calendar.md를 TASK로 변경한다.
2. member-domain: `CommonResponse`, `CommonValidateResult`와 단위 테스트를 작성한다.
3. member-api: `CommonException`과 단위 테스트를 작성한다.
4. member-api: `CommonExceptionHandler`와 단위 테스트를 작성한다. (standalone MockMvc로 예외별 상태 코드와 응답 본문 확인)
5. `spotlessApply`, 컴파일, 추가한 테스트를 실행해 검증한다.
6. 작업 내용을 task 문서에 정리한다. (요청 시)

## 제외

- 공통 요청, 공통 페이징 요청, 페이지 응답
- 핸들러 개선(`RuntimeException` 중복 제거, 500 응답의 메시지 노출 제거) - 추후 수정

---
