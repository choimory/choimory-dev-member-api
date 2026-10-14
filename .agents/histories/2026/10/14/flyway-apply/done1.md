# 목차

- [개요](#개요)
- [작업 마무리](#작업-마무리)
- [작업 요약](#작업-요약)
- [검증 결과](#검증-결과)
- [참고 사항](#참고-사항)

---

# 개요

- `member-api`에 Flyway를 적용해 스키마를 마이그레이션 SQL로 관리하도록 바꿨다.
- 마이그레이션 원본은 `docs/flyway/v1`에 두고, 기존 sql-init 방식은 제거했다.
- `member-domain`에는 Flyway를 적용하지 않고, 테스트가 같은 SQL을 버전 순으로 직접 적용한다.
- README.md에 Flyway 구조와 운영 규칙을 반영했다.

---

# 작업 마무리

> 작업 마무리하자

## 답변

작업 마무리를 진행했다. `done1.md`를 작성하고 `.agents/histories/calendar.md`의 Flyway 작업 상태를 `DONE`으로 변경했으며, `README.md`에 변경사항을 반영했다.

---

# 작업 요약

## Flyway 적용 (plan1, task1)

- `docs/flyway/v1/V1__init_schema.sql`: 기존 DDL을 복사해 첫 마이그레이션으로 만들었다. 헤더를 Flyway 규칙(적용된 버전은 수정 금지, 변경은 V2 이후 새 파일)으로 바꾸고, 기존 DB에서도 안전하도록 `if not exists`는 유지했다.
- `member-api`: `flyway-core`, `flyway-database-postgresql`을 추가하고, `processResources`가 `docs/flyway`를 classpath `db/migration` 아래로 복사하도록 바꿨다. 기존 `sql/` 복사는 제거했다.
- 설정: `application-flyway.yml`(`validate-on-migrate`, `clean-disabled`)을 만들고 `default`, `prod`, `dev`, `local` 프로필 그룹에 추가했다. `application-sql-init.yml`과 sql-init 테스트 2개는 삭제했다.
- `application-prod.yml`: `on-profile`을 `dev`에서 `prod`로 고치고 SQL 로그 설정 3개를 `false`로 바꿨다.
- `member-domain`: Flyway 의존성 없이, 테스트가 `docs/flyway`의 `V*.sql`을 버전 숫자 순으로 컨테이너에 적용하도록 `MigrationScripts`(테스트 지원 클래스), `TestcontainersConfiguration`, `DdlScriptTest`, `build.gradle`을 바꿨다.
- 테스트 추가: `FlywayMigrationTest`, `FlywayProfileTest`(member-api), `MigrationScriptsTest`(member-domain)

## 로컬 기동 오류 (task2)

- 이전 sql-init으로 테이블이 만들어진 로컬 DB에 처음 붙이자 `Found non-empty schema(s) "public" but no schema history table`로 기동이 중단되었다.
- 로컬 DB를 비우고 다시 만드는 방법으로 해결했고, baseline 설정은 추가하지 않았다.

## 점검 후 수정 (plan2, task3)

- api 테스트의 Postgres 버전을 `postgres:latest`에서 `postgres:16`으로 맞췄다.
- `CommonExceptionHandler`의 `Exception`, `AuthenticationException` 핸들러에서 응답에 노출되면 안 되는 `e.getMessage()`를 지우지 않고 한 줄 설명과 함께 주석 처리했다. 인증 예외 테스트 기대값도 맞췄다.

## 그 외 변경

- `V1__init_schema.sql`과 `docs/db`의 참고용 DDL에서 COMMENT 문을 하단에 모으지 않고 테이블별 DDL 바로 아래로 옮겼다. (COMMENT 문 100개, 내용은 그대로) 이 작업은 task 문서에 별도로 기록하지 않았다.

---

# 검증 결과

- `./gradlew clean test`로 전체 테스트 127개를 실행해 모두 통과했다. (실패 0, 오류 0, 건너뜀 0)
- 마지막 핸들러 주석 정리 이후에는 `CommonExceptionHandlerTest`, `CommonExceptionTest` 9개만 다시 실행해 통과했고, 전체 테스트는 다시 실행하지 않았다.
- `spotlessApply`로 포맷을 적용했다.
- 로컬 DB를 비우고 앱을 실행해 Flyway가 V1을 적용하고 정상 기동하는 것을 사용자가 확인했다.

---

# 참고 사항

- 이미 DDL이 적용된 dev, prod DB가 생기면 baseline 전략(`baseline-on-migrate`, `baseline-version`)이 필요하다. 현재 설정에는 없다.
- `docs/db/v1`(참고용)과 `docs/flyway/v1`의 DDL 본문이 같아, 이후 V2부터는 `docs/db`가 낡을 수 있다. README에 "참고용, 최신 스키마는 `docs/flyway` 기준"으로 명시했다. `docs/db` 헤더에는 옛 멱등 규칙이 남아 있다.
- 서비스가 여러 개인 MSA에서는 서비스별 PostgreSQL database를 따로 두고 `public` 스키마를 쓰는 구성을 기본으로 논의했다. (별도 schema 분리는 적용하지 않았다)
- `local`에서도 Flyway를 쓰며, 로컬 DB는 필요하면 비우고 다시 만든다. `clean-disabled: true`가 모든 프로필에 적용되어 있다.
- git에는 아직 아무것도 커밋하지 않았다. 삭제한 3개 파일(`application-sql-init.yml`, sql-init 테스트 2개)만 `git rm`으로 staged 상태다.
- `api-base-structure`의 핸들러 문서(plan2, task2)에는 핸들러 수정 전 상태가 남아 있고, 이후 수정은 이 디렉토리의 task3에 기록했다.

---
