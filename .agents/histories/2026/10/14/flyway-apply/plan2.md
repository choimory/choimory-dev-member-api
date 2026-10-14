# 목차

- [개요](#개요)
- [작업 점검 결과](#작업-점검-결과)

---

# 개요

-

---

# 작업 점검 결과

> 지금 작업에 더 해야하거나 거슬리는거 없지?

## 답변

- 크게 막히는 것은 없고, 마무리 전에 짚을 항목을 중요도 순으로 정리했다.

## 해야 할 것

### 1. README.md 갱신 (마무리 단계의 필수 항목)

- "DB와 실행 환경" 섹션이 아직 sql-init과 `docs/db` 복사 방식을 설명하고 있어 현재 코드와 맞지 않는다.
- `local` 프로필은 이제 Flyway를 쓰고, 마이그레이션 원본은 `docs/flyway/v1`이다.
- 기존 DB에서 baseline 오류가 날 수 있다는 점도 적어 두면 좋다.

### 2. task1에 빠진 작업

- task1에는 COMMENT를 테이블별로 나눈 작업(`V1__init_schema.sql`과 `docs/db`의 DDL)이 없다.
- `docs/db`는 수정했지만 이 작업은 어느 task에도 기록되어 있지 않아, task 문서에 추가할지 정해야 한다.

## 거슬리는 것

### 3. DDL 파일이 두 벌

- `docs/db/v1`과 `docs/flyway/v1/V1__init_schema.sql`의 본문이 같고 헤더만 다르다.
- `docs/db` 쪽 헤더에는 "앱이 뜰 때마다 실행되므로 멱등이어야 한다"는 옛 규칙이 남아 있다.
- V2부터는 `docs/flyway`에만 추가되므로 `docs/db`는 곧 낡은 참고용이 된다.
- 헤더에 "참고용, 최신 스키마는 `docs/flyway` 기준"이라고 적거나, 장기적으로 `docs/db`를 없애는 방안이 있다.

### 4. api 테스트의 Postgres 버전

- `member-api`의 `TestcontainersConfiguration`은 `postgres:latest`를 쓰고, `member-domain`은 `postgres:16`을 쓴다. 로컬 DB는 16.9이다.
- `latest`가 17 이상으로 바뀌면 테스트 환경이 달라질 수 있어 `postgres:16`으로 맞추는 것이 좋다. 이번 작업 범위 밖이라 수정하지 않았다.

### 5. git 상태

- 삭제한 파일 3개(`application-sql-init.yml`, `SqlInitProfileTest`, `LocalProfileSqlInitContextTest`)만 `git rm`으로 staged이고, 나머지 변경은 staged가 아니다.
- 아직 아무것도 커밋하지 않았다. 커밋 범위는 따로 정해야 한다.

## 이전 작업에서 남은 것 (참고)

### 6. AuthenticationException 핸들러

- `api-base-structure`의 `CommonExceptionHandler`에서 `AuthenticationException` 핸들러의 `data`에 `e.getMessage()`가 그대로 들어간다. 수정 여부는 아직 정하지 않았다.

### 7. api-base-structure 문서

- `api-base-structure`의 plan2 구현 섹션과 task2의 핸들러 표는 수정 전 상태이며, 수정 내용은 task2의 "예외 핸들러 수정" 섹션에 있다.

## 정리

- 1번은 마무리 단계에서 처리한다.
- 나머지는 4번을 이번에 같이 고칠지 정해야 한다.

---

# 추가1

- api test postgres 버전 16으로 맞춰줘
- 예외핸들러에서 Exception이랑 AuthenticationException에 e.getMessage있는거 지우지말고 주석으로 해주고, 설명 써줘 (노출되면 안되서 주석쳤다)