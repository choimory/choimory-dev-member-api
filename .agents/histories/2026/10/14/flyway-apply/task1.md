# 목차

- [개요](#개요)
- [Flyway 적용 구현](#flyway-적용-구현)

---

# 개요

- plan1에서 확정한 범위대로 `member-api`에 Flyway를 적용했다.
- 마이그레이션 SQL은 `docs/flyway/v1`에 두고, 빌드 시 classpath `db/migration` 아래로 복사해 앱 기동 시 Flyway가 실행한다.
- `member-domain`에는 Flyway를 적용하지 않고, 테스트가 같은 SQL 파일을 버전 순으로 직접 적용한다.
- 기존 sql-init 방식은 제거했고, `application-prod.yml`의 프로필 오류를 함께 수정했다.

---

# Flyway 적용 구현

> 작업시작

## 답변

- calendar.md를 `PLAN`에서 `TASK`로 변경하고 작업을 시작했다.

## 마이그레이션 파일

- `docs/flyway/v1/V1__init_schema.sql`을 만들었다. 기존 `docs/db/v1/v1-choimory-dev-member-ddl.sql`을 복사한 것이다.
- 헤더의 "local 프로필에서 앱이 뜰 때마다 실행되므로 멱등이어야 한다" 규칙을 Flyway 규칙으로 바꿨다.
  - 이미 적용된 버전은 수정하지 않고 변경은 V2 이후 새 파일로 추가한다. (수정하면 체크섬이 달라져 앱이 기동하지 않는다)
  - 기존 DDL이 적용된 DB에서도 안전하도록 `create ... if not exists`는 유지하고 DROP 문은 작성하지 않는다.
- `docs/db`는 참고용으로 남겼고 수정하지 않았다.
- DML은 아직 적을 내용이 없어 만들지 않았다. 필요할 때 `V2__...sql`로 추가한다.

## member-api

### build.gradle

- 의존성 `flyway-core`와 `flyway-database-postgresql`을 추가했다. (Flyway 10부터 DB별 모듈이 분리되어 둘 다 필요하며 버전은 Boot BOM이 관리한다)
- `processResources`가 `docs/flyway`를 classpath `db/migration` 아래로 복사하도록 바꿨다. (결과: `db/migration/v1/V1__init_schema.sql`)
- 기존 `docs/db`를 `sql/`로 복사하던 설정은 제거했다.

### 설정(yml)

- `application-flyway.yml`을 새로 만들었다. (`on-profile: flyway`)
  - `spring.flyway.enabled: true`, `locations: classpath:db/migration`, `validate-on-migrate: true`, `clean-disabled: true`
- `application.yml`의 프로필 그룹에 `flyway`를 추가했다.
  - `default: prod, flyway`, `prod: prod, flyway`, `dev: dev, flyway`, `local: local, flyway`
- `application-sql-init.yml`을 삭제했다.
- `application-prod.yml`을 수정했다.
  - `on-profile`을 `dev`에서 `prod`로 고쳤다.
  - 내용이 dev 설정의 복사본이라 `on-profile`만 고치면 prod에서 SQL 로그가 출력되기 시작하므로, `show-sql`, `show_sql`, `format_sql`을 `false`로 바꿨다.
- 기존 DB 환경과 마이그레이션 전용 계정에 대한 요청이 없어 baseline 설정은 넣지 않고 `spring.datasource`를 그대로 쓴다.

## member-domain

- Flyway 의존성은 추가하지 않았다.
- `build.gradle`: 테스트 시스템 프로퍼티를 `docs.db.ddl`(파일 하나)에서 `docs.flyway.dir`(디렉토리)로 바꿨다.
- `MigrationScripts`(테스트 지원 클래스)를 추가했다.
  - `docs.flyway.dir` 아래에서 `V{숫자}__{설명}.sql` 파일을 찾아 버전 숫자 순으로 정렬한다. (V10이 V2보다 뒤에 오도록 문자열이 아닌 숫자로 비교한다)
- `TestcontainersConfiguration`: 파일을 `001-`, `002-` 같은 번호를 붙여 `/docker-entrypoint-initdb.d`에 복사해 버전 순으로 실행되게 했다.
- `DdlScriptTest`: 멱등성 테스트가 마이그레이션 SQL 전체를 버전 순으로 다시 실행하도록 바꾸고, V1을 멱등으로 유지하는 이유를 주석에 적었다. 클래스 주석도 `docs/flyway` 기준으로 고쳤다.
- `JpaEntityTest`: 클래스 주석의 `docs/db` 언급을 `docs/flyway`로 고쳤다.

## 테스트

### 삭제

- `SqlInitProfileTest`, `LocalProfileSqlInitContextTest` (sql-init 제거로 대상이 없어짐)

### 추가

| 테스트 | 모듈 | 확인 내용 |
|---|---|---|
| `FlywayMigrationTest` | member-api | Testcontainers에서 `ddl-auto=validate`로 기동하며 Flyway 이력에 V1이 성공으로 기록되는 것, 12개 테이블 생성, 엔티티와 스키마 정합성 |
| `FlywayProfileTest` | member-api | local, dev, prod 프로필에서 `flyway` 프로필 동시 활성화, 마이그레이션 SQL의 classpath 복사, `application-flyway.yml`의 프로필과 `clean-disabled`, prod `on-profile`과 SQL 로그 꺼짐, sql-init yml 제거 |
| `MigrationScriptsTest` | member-domain | 버전 파일명 규칙에 맞는 파일만 조회, 버전 오름차순 정렬, 첫 마이그레이션이 `V1__init_schema.sql` |

## 검증

- `spotlessApply`로 포맷을 적용했고 `spotlessCheck`가 통과했다.
- `./gradlew clean test`로 전체 테스트 127개를 실행해 모두 통과했다. (실패 0, 오류 0, 건너뜀 0)
- 기존 `ChoimoryDevMemberApiApplicationTests`가 기본 프로필(prod, flyway)로 컨텍스트를 띄우므로 이 테스트도 마이그레이션 적용을 함께 확인한다.

## 남은 일

- README.md의 "DB와 실행 환경" 섹션(sql-init 설명)과 `docs/db` 구조 설명은 아직 갱신하지 않았다. 작업 마무리 단계에서 반영한다.
- 삭제한 3개 파일(`application-sql-init.yml`, 테스트 2개)은 `git rm`으로 삭제가 staged 상태이며, 나머지 변경은 staged가 아니다.

---
