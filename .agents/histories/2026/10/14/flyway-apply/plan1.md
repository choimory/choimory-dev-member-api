# 목차

- [개요](#개요)
- [Flyway 도입 시 해야 할 것](#flyway-도입-시-해야-할-것)
- [추가1](#추가1)
- [범위 확정](#범위-확정)
- [마이그레이션의 의미](#마이그레이션의-의미)
- [docs/db/flyway 경로 질문과 정정](#docsdbflyway-경로-질문과-정정)
- [마이그레이션 파일 작성 위치](#마이그레이션-파일-작성-위치)

---

# 개요

-

---

# Flyway 도입 시 해야 할 것

> 이 프로젝트에 flyway하려면 해야할것들이 뭐가 있는지 한번 확인해봐

## 답변

- 코드와 설정을 읽어 확인한 결과이며, 수정한 것은 없다.

## 현재 상태

### 스키마 적용 방식

- `docs/db/v1/choimory-dev-member-ddl-v1.sql` 하나(360줄, 테이블 12개, 멱등 `if not exists`)가 원본이다.
- 빌드 시 `processResources`가 `docs/db`를 `member-api`의 classpath `sql/` 아래로 복사한다.
- `local` 프로필의 `application-sql-init.yml`이 앱 기동 때마다 그 DDL을 실행한다.
- dev와 prod는 스키마를 자동 생성하지 않는다. (`ddl-auto: none`)
- DML(`choimory-dev-member-dml-v1.sql`)은 비어 있다.

### 테스트의 DDL 사용

- `member-domain`은 시스템 프로퍼티 `docs.db.ddl`로 DDL을 Postgres 컨테이너 초기화 스크립트에 넣고, `DdlScriptTest`가 멱등성을 검증한다.
- `member-api`의 `LocalProfileSqlInitContextTest`와 `SqlInitProfileTest`는 sql-init 동작을 검증한다.

### 환경

- Flyway 의존성은 없다. Boot 3.5.4 SNAPSHOT, PostgreSQL 16을 사용한다.

## 해야 할 것

### 1. 의존성 추가

- `member-api`에 `flyway-core`와 `flyway-database-postgresql`을 추가한다. (Flyway 10부터 DB별 모듈이 분리되어 둘 다 필요하며, 버전은 Boot BOM이 관리한다)

### 2. 마이그레이션 파일 구성

- Flyway는 `V1__설명.sql` 같은 이름 규칙이 필요하다. 현재 `choimory-dev-member-ddl-v1.sql`은 맞지 않아 이름을 바꿔야 한다. (예: `V1__init_schema.sql`)
- 파일 위치를 정해야 한다.
  - (A) `member-api/src/main/resources/db/migration`으로 옮긴다.
  - (B) `docs/db`를 원본으로 두고 `processResources`로 `db/migration`에 복사한다.
  - (C) `member-domain`에 둔다. 스키마가 도메인 소유이고 domain 테스트도 쓰기 때문이다.

### 3. 기존 sql-init 제거

- `application-sql-init.yml`과 `local` 프로필 그룹의 `sql-init`을 제거한다.
- `LocalProfileSqlInitContextTest`와 `SqlInitProfileTest`는 Flyway 기준 테스트로 교체한다.
- 기존 `processResources`의 `sql/` 복사 설정을 정리한다.

### 4. Flyway 설정(yml)

- 공통 설정으로 `spring.flyway.enabled`, `locations`, `validate-on-migrate`를 정한다.
- prod는 `clean-disabled: true`로 둔다. (Flyway 10+는 기본값이 true라 확인만 하면 된다)
- Flyway는 기본적으로 `spring.datasource`를 쓰므로, 마이그레이션 전용 DB 계정이 필요하면 `spring.flyway.user`와 `password`를 따로 둔다.

### 5. 기존 DB 처리

- 이미 DDL이 적용된 DB(로컬이나 dev)가 있으면 `baseline-on-migrate`나 `baseline-version` 전략이 필요하다.
- V1이 `if not exists`라 그대로 실행해도 안전하지만, Flyway 이력 테이블(`flyway_schema_history`) 처리는 정해야 한다.

### 6. DDL 파일 내용 정리

- 헤더의 "local 프로필에서 앱이 뜰 때마다 실행되므로 멱등이어야 한다, DROP 금지" 규칙은 Flyway에서는 의미가 달라진다.
- 이미 적용된 버전은 수정할 수 없고(체크섬 검증), 변경은 V2 이후 새 파일로만 한다.
- `if not exists`를 유지할지 제거할지 정해야 한다.

### 7. DML 처리

- 빈 파일은 그대로 Flyway에 넣지 않는 것이 안전하고, 초기 데이터가 생길 때 `V2__...sql`로 추가한다. (Flyway의 빈 SQL 파일 처리는 확인하지 않았다)
- 이름은 `Vn`(버전)과 `Rn`(반복) 중 어느 쪽이 맞을지 정해야 한다.

### 8. 테스트 변경

- `member-domain`: 컨테이너에 DDL을 직접 넣는 방식(`docs.db.ddl`)을 새 파일 위치와 이름에 맞춰 수정해야 한다. `DdlScriptTest`의 멱등성 테스트도 재정의가 필요하다.
- `member-api`: Testcontainers로 Flyway가 실제 마이그레이션되고, `ddl-auto=validate`와 엔티티 정합성이 맞는지 검증하는 테스트를 추가한다.

### 9. 문서

- README의 "DB와 실행 환경" 섹션(sql-init 설명)과 `docs/db` 구조를 갱신한다.

## 같이 확인된 문제

- `application-prod.yml`의 `on-profile`이 `prod`가 아니라 `dev`로 되어 있다.
  - prod 프로필의 설정(`logging.root: error` 등)이 적용되지 않고 dev 설정과 겹친다.
  - Flyway 설정을 프로필별로 나누려면 먼저 고쳐야 한다. 이번 작업 범위에 넣을지 별도로 고칠지 정해야 한다.
- 마이그레이션 디렉토리가 아직 없어서 Flyway 기동 시 경로가 없으면 오류가 난다. 파일을 먼저 두거나 `fail-on-missing-locations` 설정을 고려해야 한다.

## 결정이 필요한 것

1. 마이그레이션 파일 위치: A, B, C 중 어느 쪽인가. (C `member-domain`을 추천하지만, `member-api`만 마이그레이션을 실행해야 하는지에 따라 달라진다)
2. 이미 DDL이 적용된 dev나 prod 환경이 있는가. (baseline 전략에 영향)
3. `local`에서도 Flyway를 쓰고 sql-init은 완전히 제거하는 것이 맞는가.
4. 마이그레이션 전용 DB 계정을 분리할 계획이 있는가.
5. `application-prod.yml` 버그를 이번 작업에 포함할 것인가.

---

# 추가1

- docs/db는 그냥 참고용으로 남기고, flyway용 sql은 별도의 디렉토리에 만들자. docs/flyway/v1 여기에 만들고 사용할 수 있으면 여기가 좋은데 불가능하면 가능한곳에 아무대나 만들자
- member-domain에는 flyway 적용하기 싫고 member-api쪽에만 하고 싶은데 가능한가
- sql-init 필요없으면 지우고 flyway용 yml만들어서 프로필 그룹에 추가하자
- dml은 아직 적을거 없으니까 필요할때 만들자
- 테스트는 알아서 작성
- application prod on profile 수정해줘

---

# 범위 확정

> 1번 그렇게 하고

## 답변

- 추가1의 결정과 "prod 설정 내용" 확인 사항(1번)을 반영해 범위를 확정한다.

## 확정 내용

### 마이그레이션 파일

- Flyway용 SQL은 `docs/flyway/v1`에 둔다. 파일 이름은 Flyway 규칙에 맞춰 `V1__init_schema.sql`로 한다.
- `member-api`의 `processResources`가 `docs/flyway`를 classpath `db/migration` 아래로 복사한다. (결과: `db/migration/v1/V1__init_schema.sql`)
  - Flyway는 `classpath:db/migration`을 하위 디렉토리까지 재귀로 스캔하므로 `v1` 하위에 둬도 동작한다.
  - `filesystem:` 경로는 실행 위치에 따라 달라지고 jar 배포에서 깨지기 쉬워 사용하지 않는다.
- `docs/db`는 참고용으로 남기고, 기존 `sql/` 복사 설정은 제거한다.
- DML은 아직 적을 내용이 없으므로 필요할 때 `V2__...sql`로 만든다.

### 적용 모듈

- Flyway 의존성(`flyway-core`, `flyway-database-postgresql`)은 `member-api`에만 추가한다. `member-domain`에는 Flyway를 적용하지 않는다.
- `member-domain` 테스트의 스키마 원본은 `docs/flyway/v1`의 파일로 바꾼다. (`docs/db`를 참고용으로 남기면 DDL이 두 벌이 되어 어긋날 수 있다)
  - 이후 `V2`가 생기는 경우를 대비해, 파일 하나가 아니라 디렉토리의 `V*.sql`을 버전 순으로 모두 적용하는 방식으로 바꾼다.

### 설정과 프로필

- sql-init 관련 설정(`application-sql-init.yml`)과 테스트(`LocalProfileSqlInitContextTest`, `SqlInitProfileTest`)는 삭제한다.
- `application-flyway.yml`(`on-profile: flyway`)을 만들고 `local`, `dev`, `prod` 프로필 그룹에 모두 추가한다.
- 기존 DB 환경과 마이그레이션 전용 계정에 대한 별도 요청이 없어서, baseline 설정은 넣지 않고 `spring.datasource`를 그대로 쓴다. (기존 DDL이 적용된 DB가 있으면 그때 baseline 전략을 추가한다)

### application-prod.yml

- `on-profile`을 `dev`에서 `prod`로 수정한다.
- 현재 내용이 dev 설정의 복사본이라 `on-profile`만 고치면 prod에서 SQL 로그가 출력되기 시작하므로, `show-sql`, `show_sql`, `format_sql`을 `false`로 함께 바꾼다.

### 테스트

- 테스트는 알아서 작성한다.
  - `member-api`: Testcontainers로 Flyway가 실제로 마이그레이션되는지, 엔티티와 스키마 정합성(`ddl-auto=validate`)이 맞는지 검증한다.
  - `member-domain`: 스키마 원본 경로 변경과 `V*.sql` 순서 적용에 맞게 수정한다. `DdlScriptTest`의 멱등성 테스트는 새 기준에 맞게 재정의한다.
  - 프로필 그룹과 prod `on-profile` 수정에 대한 설정 검증을 추가한다.

---

# 마이그레이션의 의미

> 마이그레이션 은 뭐지?

## 답변

- 마이그레이션(migration)은 DB 스키마의 변경을 코드처럼 버전으로 관리하고 순서대로 적용하는 방식이다.

## 기본 개념

- DB에 테이블을 만들거나 컬럼을 추가하는 SQL을 `V1__init_schema.sql`, `V2__add_column.sql`처럼 버전이 붙은 파일로 둔다.
- Flyway가 앱이 뜰 때 DB에 어떤 버전까지 적용됐는지 확인하고, 적용되지 않은 파일만 버전 순서대로 실행한다.
- 적용 이력은 DB의 `flyway_schema_history` 테이블에 기록된다.

## 현재 방식(sql-init)과의 차이

| | 현재 (sql-init) | Flyway |
|---|---|---|
| 실행 시점 | `local`에서 앱이 뜰 때마다 DDL 전체를 실행 | 앱이 뜰 때 아직 적용되지 않은 파일만 한 번씩 실행 |
| 변경 방식 | DDL 파일을 직접 수정하고 `if not exists`로 멱등하게 작성 | 이미 적용된 파일은 수정하지 않고 새 버전 파일(V2, V3...)을 추가 |
| 환경 | `local`만 | local, dev, prod 모두 같은 방식 |
| 이력 | 없음 | DB에 어떤 변경이 언제 적용됐는지 기록 |

## 예시

1. 처음에는 `V1__init_schema.sql`에 현재의 12개 테이블 DDL이 들어간다.
2. 나중에 `member`에 컬럼을 추가해야 하면 V1을 고치지 않고 `V2__add_member_phone.sql`에 `alter table member add column phone ...`을 쓴다.
3. 앱이 뜨면 Flyway가 DB가 V1까지 적용된 것을 보고 V2만 실행한다.

- V1을 수정하면 파일의 체크섬이 이력과 달라져서 기동이 실패한다.
- "마이그레이션 전용 DB 계정"은 이 SQL을 실행하는 데 쓰는 계정(보통 테이블 생성 권한이 있는 계정)을 뜻하며, 이번에는 앱 계정과 분리하지 않는다.

---

# docs/db/flyway 경로 질문과 정정

> docs/db/flyway에 두는건 안되나?

## 답변

- 이 질문에 대해 `docs/db/flyway`로 위치를 바꿔도 동작에는 차이가 없다고 답했다. `processResources`가 복사하는 원본 디렉토리만 바꾸면 되고, 구조는 `docs/db/v1`(참고용)과 `docs/db/flyway/v1`(Flyway용)이 같은 부모 아래에 있는 형태였다.

## 정정

> 내가 말한 경로는 docs/flyway/v1임

- 확정된 경로는 `docs/flyway/v1`이며, 앞의 `docs/db/flyway` 답변은 경로를 잘못 이해한 것이다.
- `docs/flyway/v1`을 그대로 쓰는 데 문제가 없다.
  - 구조는 `docs/flyway/v1/V1__init_schema.sql`이고 `docs/db`는 참고용으로 남긴다.
  - `member-api`의 `processResources`가 `docs/flyway`를 `db/migration` 아래로 복사한다. (결과: `db/migration/v1/V1__init_schema.sql`)
  - `docs/db`와 부모 디렉토리가 달라서 참고용과 Flyway 원본이 섞이지 않는다.
- 범위 확정 섹션의 내용은 바뀌지 않는다.

---

# 마이그레이션 파일 작성 위치

> 그럼 마이그레이션은 docs/flyway/v1에 적으면 되는거?

## 답변

- 맞다. 마이그레이션 SQL은 `docs/flyway/v1`에 적는다. 앱이 뜰 때 Flyway가 읽는 파일은 `processResources`가 `docs/flyway`를 classpath `db/migration`으로 복사한 것이다.

## 파일을 쓰는 방법

- 파일 이름은 `V{버전}__{설명}.sql` 형식이며, 버전 뒤에 밑줄이 두 개다.
  - 예: `V1__init_schema.sql`, `V2__add_member_phone.sql`
  - 이 이름 규칙에 맞지 않는 파일은 Flyway가 무시하거나 오류를 낸다.
- 버전 번호가 실행 순서다. V1, V2, V3 순으로 적용된다.
- 이미 적용된 파일(예: V1)은 고치지 않고, 변경이 생기면 새 번호의 파일을 추가한다.

## 구현 시작 시 할 일

- 현재 `docs/flyway/v1`은 아직 없다. 구현이 시작되면 `docs/db/v1/choimory-dev-member-ddl-v1.sql`을 `V1__init_schema.sql`로 복사해 첫 마이그레이션으로 둔다.
- 이후 스키마 변경은 `docs/flyway/v1`에 `V2__...sql`처럼 새 파일을 추가해서 적는다.
- `v1` 디렉토리는 버전 폴더이고 파일 안의 `V1`, `V2` 같은 버전과는 별개다. 나중에 `docs/flyway/v2` 디렉토리를 만들어도 파일 버전 번호는 이어서 올라간다. (Flyway가 디렉토리 이름은 구분에 쓰지 않는다)

---
