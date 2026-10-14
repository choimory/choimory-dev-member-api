# 목차

- [개요](#개요)
- [공용 응답과 예외 핸들러 구현](#공용-응답과-예외-핸들러-구현)
- [예외 핸들러 수정](#예외-핸들러-수정)

---

# 개요

- plan2에서 확정한 공용 응답, 공용 예외, 예외 핸들러를 구현한다.
- 공용 응답 객체는 member-domain에, 공용 예외와 예외 핸들러는 member-api에 둔다.
- 공통 요청, 공통 페이징 요청, 페이지 응답은 범위에서 제외한다.
- 예외 핸들러는 예전 코드(8754490d)를 그대로 옮기고, 수정은 이후에 한다.

---

# 공용 응답과 예외 핸들러 구현

> 작업계획 plan2에 작성하고 작업시작

## 답변

- plan2에 "작업 계획" 섹션을 작성하고 calendar.md를 `TASK`로 변경한 뒤 작업을 시작했다.

## 계획 대비 변경

- **데이터 객체는 record로 작성했다.**
  - 프로젝트가 `record + @Builder(toBuilder = true)` 기준이므로 `CommonResponse`, `CommonValidateResult`를 record로 옮겼다.
  - record는 component 이름과 JSON 필드명이 같아 `@JsonProperty`와 `@JsonCreator`가 필요 없다.
  - 따라서 member-domain에 Jackson 의존성을 추가하지 않았다. (plan2 앞 섹션의 "Jackson 의존성 추가"를 대체)
  - `CommonException`은 record 전환 제외 대상(Exception)이므로 class로 두었다.
- **`CommonValidateResult`를 `common/command/response`에 두었다.**
  - `common/command/dto`는 Entity와 1:1로 매칭되는 DTO 전용이고 `DtoRulesTest`가 이 패키지를 검사하므로 `response` 패키지로 정했다.

## 생성 내용

| 모듈 | 클래스 | 내용 |
|---|---|---|
| member-domain (`dev.choimory.member.domain.common.command.response`) | `CommonResponse<T>` | `code`, `name`, `message`, `data` record. 데이터 없는 생성자 포함 |
| | `CommonValidateResult` | `field`, `rejectValue`, `message` record |
| member-api (`dev.choimory.member.api.common.exception`) | `CommonException` | `RuntimeException` 상속. `status`, `code`, `codeName`, `msg`. HTTP 상태만 받는 생성자와 세부 값을 받는 생성자 |
| | `CommonExceptionHandler` | `@RestControllerAdvice("dev.choimory")`. 6개 예외 처리 |

### CommonExceptionHandler 처리 예외

| 예외 | 응답 |
|---|---|
| `Exception` | 500, `data`에 예외 메시지 |
| `RuntimeException` | 500, `data`에 예외 메시지 |
| `CommonException` | 예외에 지정된 상태와 값. 값이 없으면 HTTP 상태의 기본값 |
| `MethodArgumentNotValidException` | 400, `data`에 `CommonValidateResult` 목록 |
| `ConstraintViolationException` | 400, `data`에 `CommonValidateResult` 목록 |
| `AuthenticationException` | 401, `data`에 예외 메시지 |

- 예전 코드 그대로 옮겼으며, 아래 두 가지는 수정 대상으로 남겨 두었다.
  - `Exception`과 `RuntimeException` 핸들러가 둘 다 500을 반환하는 중복
  - 500 응답 `data`에 `e.getMessage()`를 그대로 내보내는 부분
- 클래스, 필드, 함수에 Doc Comment를 작성하고 함수 내부에는 흐름별 한글 주석을 작성했다.

## 테스트

| 테스트 | 개수 | 확인 내용 |
|---|---|---|
| `CommonResponseTest` | 3 | 데이터 없는 생성자(data null), Builder, toBuilder |
| `CommonValidateResultTest` | 2 | Builder, toBuilder |
| `CommonExceptionTest` | 2 | 상태만 지정한 기본값, 세부 값 지정 |
| `CommonExceptionHandlerTest` | 7 | 예외 6종의 상태 코드와 응답 본문 (`CommonException`은 상태만 지정한 경우와 세부 값을 지정한 경우 2건) |

- `CommonExceptionHandlerTest`는 예외를 던지는 테스트용 컨트롤러와 standalone MockMvc를 사용한다.

## 검증

- `spotlessApply`를 실행해 포맷을 적용했다.
- 위 테스트 14개와 `DtoRulesTest` 4개를 실행해 모두 통과했다. (전체 테스트는 실행하지 않았다)

---

# 예외 핸들러 수정

> 1,2번 반영해줘

## 답변

- 앞서 짚은 수정 후보 두 가지(`Exception`과 `RuntimeException` 핸들러 중복, 500 응답의 `e.getMessage()` 노출)를 `CommonExceptionHandler`에 반영했다.

## 변경 내용

### RuntimeException 핸들러 제거

- `RuntimeException`은 `Exception`의 하위 타입이라 `Exception` 핸들러가 처리하므로 별도 핸들러를 제거했다.
- `CommonException`은 `RuntimeException`의 하위 타입이지만, Spring은 가장 가까운 타입의 핸들러를 선택하므로 `CommonException` 핸들러가 우선한다. 동작은 달라지지 않는다.

### 500 응답에서 예외 메시지 제거

- `Exception` 핸들러는 응답 `data`에 `e.getMessage()`를 넣지 않는다. 예외는 로그로만 남기고, 응답은 `code`, `name`, `message`만 내려간다.
- DB 오류나 내부 클래스 정보 같은 내부 메시지가 클라이언트에 노출되는 것을 막기 위함이다.
- 핸들러의 Doc Comment에 이 의도를 적었다.

### 유지한 부분

- `AuthenticationException` 핸들러는 `data`에 `e.getMessage()`를 넣는 방식을 그대로 두었다. (수정 여부는 추후 결정)

## 테스트 수정

- 500 응답 테스트 2개(`Exception`, `RuntimeException`)의 기대값을 `data`가 없는 것으로 바꿨다.
- `RuntimeException` 테스트는 `Exception` 핸들러를 거쳐 500이 나오며 예외 메시지를 노출하지 않는 것을 확인한다.

## 검증

- `spotlessApply`를 실행하고 `CommonExceptionHandlerTest` 7개, `CommonExceptionTest` 2개를 실행해 모두 통과했다. (전체 테스트는 실행하지 않았다)
- 위 "공용 응답과 예외 핸들러 구현" 섹션의 핸들러 표(`Exception`, `RuntimeException`의 `data`에 예외 메시지)는 수정 전 상태를 기록한 것이다.

---
