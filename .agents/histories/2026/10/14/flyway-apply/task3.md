# 목차

- [개요](#개요)
- [api 테스트 Postgres 버전과 예외 핸들러 메시지 주석 처리](#api-테스트-postgres-버전과-예외-핸들러-메시지-주석-처리)

---

# 개요

- plan2의 "작업 점검 결과"와 "추가1"에서 정리된 두 가지 항목을 반영했다.
- api 테스트의 Postgres 버전을 16으로 맞추고, 예외 핸들러에서 응답에 노출되면 안 되는 `e.getMessage()`를 주석 처리했다.
- 예외 핸들러는 `api-base-structure`에서 만든 `CommonExceptionHandler`이며, 이 작업은 그 수정이다.

---

# api 테스트 Postgres 버전과 예외 핸들러 메시지 주석 처리

> api test postgres 버전 16으로 맞춰줘
>
> 예외핸들러에서 Exception이랑 AuthenticationException에 e.getMessage있는거 지우지말고 주석으로 해주고, 설명 써줘 (노출되면 안되서 주석쳤다)

## 답변

- 위 두 항목을 확인하고 계획을 설명한 뒤, 진행 지시에 따라 반영했다.

## 발생한 상황

- 작업 전 점검에서 `CommonExceptionHandler`의 `Exception` 핸들러(41행)와 `AuthenticationException` 핸들러(129행)에 `e.getMessage()`가 응답 `data`로 살아 있는 것을 확인했다.
- 이전에 `Exception` 핸들러의 메시지 제거를 반영했으나, 점검 시점의 파일에는 그 변경이 보이지 않았다. 원인은 확인하지 못했다.
- 이번 수정으로 파일 안의 `getMessage` 호출은 검증 실패 처리의 `violation.getMessage()`만 남았다.

## 변경 내용

### api 테스트 Postgres 버전

- `member-api`의 `TestcontainersConfiguration`을 `postgres:latest`에서 `postgres:16`으로 바꿨다.
- `member-domain`의 테스트(`postgres:16`), 로컬 DB(PostgreSQL 16.9)와 같은 버전이 되어 `latest`가 17 이상으로 바뀌어도 테스트 환경이 달라지지 않는다.

### 예외 핸들러의 메시지 주석 처리

- 대상: `CommonExceptionHandler`의 `Exception` 핸들러와 `AuthenticationException` 핸들러
- `e.getMessage()` 인자를 지우지 않고 `// , e.getMessage()` 주석으로 남기고, 데이터 없는 3개 인자 생성자를 사용하도록 했다.
- 각 핸들러에 주석으로 이유를 설명했다.
  - `Exception`: 처리되지 않은 예외의 메시지에는 DB 오류, 쿼리, 클래스 정보 같은 내부 정보가 담길 수 있어 클라이언트에 노출되면 안 된다. 예외는 로그로만 남긴다.
  - `AuthenticationException`: 인증 예외의 메시지에는 실패 사유(계정 존재 여부, 내부 처리 정보 등)가 담길 수 있어 노출되면 안 된다.
  - 노출이 꼭 필요하면 주석을 풀고 데이터 없는 생성자 대신 4개 인자 생성자를 사용한다고 적었다.

### 테스트 수정

- `CommonExceptionHandlerTest`의 인증 예외 테스트 기대값을 `data`가 `"bad credentials"`인 것에서 `data`가 없는 것으로 바꿨다.
- 메서드 설명을 "인증 예외는 401 응답으로 변환되며 예외 메시지를 노출하지 않습니다."로 고쳤다.
- `Exception`, `RuntimeException` 테스트는 이미 `data`가 없는 것을 기대하고 있어 수정하지 않았다.

## 검증

- `spotlessApply`를 실행해 포맷을 적용했다.
- `./gradlew clean test`로 전체 테스트 127개를 실행해 모두 통과했다. (실패 0, 오류 0)

## 남은 일

- README.md 갱신과 done 문서 작성이 마무리 단계로 남아 있다.
- 기록 위치: 이 변경은 `api-base-structure`의 핸들러 수정이지만, 사용자 지시에 따라 flyway-apply의 task3에 기록했다.

---
