# 목차

- [개요](#개요)
- [작업 마무리](#작업-마무리)
- [작업 요약](#작업-요약)
- [검증 결과](#검증-결과)
- [참고 사항](#참고-사항)

---

# 개요

- done1.md 이후에 진행한 추가 작업들을 정리한다. (plan2, plan3, task3, task4)
- api 테스트의 Postgres 버전을 16으로 맞추고, 예외 핸들러에서 노출되면 안 되는 `e.getMessage()`를 주석 처리했다.
- 모든 시각 컬럼을 `timestamp`에서 `timestamptz`로 바꾸고, 엔티티와 DTO의 시각 타입을 `LocalDateTime`에서 `Instant`로 바꿨다. DB 변경은 V1을 수정하지 않고 Flyway의 `V2` 마이그레이션으로 처리했다.
- Testcontainers와 timestamp 타입에 대한 질문과 답변은 plan3에 정리했다.

---

# 작업 마무리

> 작업 마무리하자.

## 답변

작업 마무리를 진행했다. 추가 작업을 `done2.md`로 작성하고 `.agents/histories/calendar.md`의 Flyway 작업 상태를 다시 `DONE`으로 변경했으며, `README.md`는 작업 중에 이미 반영한 내용(V2, `Instant`와 `timestamptz` 규칙, 새 테스트)을 확인했다.

---

# 작업 요약

## 점검 후 수정 (plan2, task3)

- api 테스트의 Postgres 버전을 `postgres:latest`에서 `postgres:16`으로 맞췄다. `member-domain` 테스트, 로컬 DB(PostgreSQL 16.9)와 같은 버전이다.
- `CommonExceptionHandler`의 `Exception`, `AuthenticationException` 핸들러에서 응답 `data`로 나가던 `e.getMessage()`를 지우지 않고 주석 처리했다. 처음에는 긴 설명 주석을 달았고, 이후 `e.getMessage()` 옆의 한 줄 주석으로 줄였다.
  - `Exception`: 내부 정보(DB 오류, 쿼리, 클래스 정보 등)가 노출되면 안 되어 응답에서 제외
  - `AuthenticationException`: 인증 실패 사유(계정 존재 여부 등)가 노출되면 안 되어 응답에서 제외
- 인증 예외 테스트의 기대값을 `data`가 없는 것으로 바꿨다.

## timestamptz 변경 (plan3, task4)

- `docs/flyway/v1/V2__timestamp_to_timestamptz.sql`을 새로 만들었다.
  - 테이블 12개의 시각 컬럼 38개를 `alter column ... type timestamptz using ... at time zone 'Asia/Seoul'`로 바꾼다.
  - 기존 `timestamp` 값은 서버 시간대(Asia/Seoul)로 쓴 값이므로 시간대를 명시해 올바른 시각으로 변환한다.
  - 이미 적용된 V1은 체크섬 때문에 수정하지 않고 새 버전으로 추가했다.
- `docs/db/v1`의 참고용 DDL은 직접 `timestamptz`로 바꿨다. (38곳, 컬럼 정렬 포함)
- 엔티티와 DTO 14개 파일의 `LocalDateTime`을 `Instant`로 바꿨다. (`BaseEntity`, `MemberSuspension`, DTO 12개)
  - Hibernate 6가 `Instant`를 `timestamptz`로 매핑하고 Spring Data 감사도 지원해서 매핑 설정은 추가하지 않았다.
- README.md에 V2 설명, 엔티티 시각 규칙(JSON은 UTC 기준 ISO 문자열, 달력 계산은 `ZoneId`를 붙여 변환), 새 테스트를 반영했다.

## 테스트 변경

- 기존 테스트 수정: `MemberDtosTest`, `MemberEntitiesTest`, `BaseEntityTest`를 `Instant` 기준으로 고쳤다.
- 새 테스트
  - `DdlScriptTest.timeColumnsAreTimestamptz`: 시간대 없는 `timestamp` 컬럼이 없고 `timestamptz` 컬럼이 38개인지 확인
  - `FlywayTimestamptzMigrationTest`(member-api): V1까지만 적용해 서울 09:00 값을 저장한 뒤 V2를 적용하면 컬럼이 `timestamp with time zone`으로 바뀌고 값이 UTC `00:00`으로 변환되는지 확인

## 논의한 내용 (plan3)

- Testcontainers: 사용 방법, 컨테이너의 실행 위치(테스트는 호스트에서, Postgres만 Docker에서)와 정리(Ryuk), 일반 Spring Boot 테스트와 같은 실행 방식, DB 컨테이너 설정의 기준(엔티티가 아니라 `docs/flyway`의 SQL)
- Flyway가 없는 프로젝트의 스키마 구성 방식 4가지와 비교
- `timestamp`와 `timestamptz`의 차이와 권장 사용
- `Instant`를 권하는 이유와 `OffsetDateTime`과의 비교

---

# 검증 결과

- `./gradlew clean test`로 전체 테스트 129개를 실행해 모두 통과했다. (기존 127개에 새 테스트 2개, 실패 0, 오류 0, 건너뜀 0)
- `spotlessApply`로 포맷을 적용했다.
- 한 줄 주석으로 줄인 직후에는 핸들러 테스트 9개만 실행했고, 이후 `timestamptz` 변경 작업에서 전체 테스트를 다시 실행해 통과했다.
- 로컬 DB에 앱을 실행해 V2가 적용되는 것은 이 작업에서 확인하지 않았다. 다음에 앱을 실행하면 Flyway가 V2를 자동으로 적용한다.

---

# 참고 사항

- V2는 기존 값을 `Asia/Seoul` 기준으로 변환한다. 다른 시간대의 서버가 쓴 데이터가 있는 환경에는 이 가정이 맞지 않는다.
- `docs/db/v1`의 참고용 DDL 헤더에는 "앱이 뜰 때마다 실행되므로 멱등이어야 한다"는 옛 규칙이 남아 있다. 이 파일은 참고용이며 최신 스키마는 `docs/flyway` 기준이라고 README에 명시했다.
- `AuthenticationException` 핸들러의 `e.getMessage()`는 주석으로 남아 있어, 필요하면 주석을 풀고 4개 인자 생성자를 쓰면 된다.
- 이미 DDL이 적용된 dev, prod DB가 생기면 baseline 전략(`baseline-on-migrate`, `baseline-version`)이 필요하다. 현재 설정에는 없다.
- 달력 규칙이 필요한 계산(예: 정지 기간을 한 달 뒤로 계산)은 `Instant`에 `ZoneId`를 붙여 변환해서 처리한다.
- git에는 아직 아무것도 커밋하지 않았다. 삭제한 3개 파일(`application-sql-init.yml`, sql-init 테스트 2개)만 `git rm`으로 staged 상태다.
- `api-base-structure`의 핸들러 문서(plan2, task2)에는 핸들러 수정 전 상태가 남아 있고, 이후 수정은 이 디렉토리의 task3에 기록했다.

---
