# 목차

- [개요](#개요)
- [timestamptz 변경 구현](#timestamptz-변경-구현)

---

# 개요

- plan3에서 정리한 timestamptz 변경을 구현했다.
- DB는 이미 적용된 V1을 수정하지 않고 `V2__timestamp_to_timestamptz.sql`을 새로 추가해 Flyway로 변경했다. (Flyway를 실제 변경에 처음 사용)
- 엔티티와 DTO의 시각 타입은 `LocalDateTime`에서 `Instant`로 바꿨다.
- 이 작업은 이미 DONE이던 `flyway-apply`를 이어서 진행하며, calendar.md를 `TASK`로 되돌렸다.

---

# timestamptz 변경 구현

> Instant로 하고 Asia/Seoul로 작업 시작해줘

## 답변

- 정하지 않은 항목 중 타입은 `Instant`, V2의 변환 시간대는 `Asia/Seoul`로 확정하고 작업을 시작했다.
- 작업 디렉토리는 따로 정해지지 않아 별도 디렉토리를 만들지 않고 `flyway-apply`를 이어서 사용했다.

## DB

### V2 마이그레이션

- `docs/flyway/v1/V2__timestamp_to_timestamptz.sql`을 새로 만들었다.
- 테이블 12개의 시각 컬럼 38개(`created_at`, `modified_at`, `deleted_at` 36개와 `member_suspension`의 `suspended_at`, `suspended_to` 2개)를 테이블별로 한 번의 `alter table`로 바꾼다.

```sql
alter table member
    alter column created_at type timestamptz using created_at at time zone 'Asia/Seoul',
    alter column modified_at type timestamptz using modified_at at time zone 'Asia/Seoul',
    alter column deleted_at type timestamptz using deleted_at at time zone 'Asia/Seoul';
```

- 기존 `timestamp` 값은 앱이 서버 시간대(Asia/Seoul)로 쓴 값이므로 `at time zone 'Asia/Seoul'`로 올바른 시각으로 변환한다. 시간대를 명시하지 않으면 실행 시점의 세션 시간대로 해석되어 DB 환경마다 결과가 달라진다.
- 파일 헤더에 변경 이유와 위 내용을 주석으로 적었다.
- 타입 변경은 COMMENT에 영향이 없어 COMMENT 문은 다시 쓰지 않았다.

### V1은 수정하지 않음

- V1은 로컬 DB에 이미 적용되어 있고, 수정하면 체크섬이 달라져 기동이 실패한다. 변경은 새 버전 파일로 추가하는 Flyway 규칙을 따랐다.

### 참고용 DDL

- `docs/db/v1/v1-choimory-dev-member-ddl.sql`은 직접 `timestamptz` 38곳으로 바꿨다.
- 타입 길이가 길어져 어긋난 컬럼 정렬(`not null` 앞 공백)도 맞췄다.

## 엔티티와 DTO

- `LocalDateTime`을 `Instant`로 바꿨다. (14개 파일)
  - 엔티티: `BaseEntity`(`createdAt`, `modifiedAt`, `deletedAt`), `MemberSuspension`(`suspendedAt`, `suspendedTo`)
  - DTO 12개: `MemberDto`, `MemberImageDto`, `MemberSuspensionDto`, `FollowDto`, `AuthorityDto`, `MemberAuthorityDto`, `MenuDto`, `PermissionDto`, `PermissionActionDto`, `AgreementDto`, `AgreementAnswerDto`, `MemberAgreementDto`
- `BaseEntity.softDelete()`는 `Instant.now()`를 쓴다.
- Hibernate 6가 `Instant`를 `timestamptz`로 매핑하고 Spring Data 감사(`@CreatedDate`, `@LastModifiedDate`)도 `Instant`를 지원하므로 매핑 설정은 추가하지 않았다.
- `Instant`를 쓰는 이유(DB의 UTC 정규화와 의미 일치, JVM 시간대와 무관, 비교 안정성, JSON 직렬화)는 plan3의 "Instant를 권하는 이유"에 정리했다.

## 테스트

### 기존 테스트 수정

| 테스트 | 변경 |
|---|---|
| `MemberDtosTest` | `LocalDateTime.of(...)`를 `Instant.parse("...T00:00:00Z")`로 변경 |
| `MemberEntitiesTest` | `LocalDateTime.now()`를 `Instant.now()`로, `plusDays(1)`을 `plus(Duration.ofDays(1))`로, `LocalDateTime.of(...)`를 `Instant.parse(...)`로 변경 |
| `BaseEntityTest` | 시각 비교 변수를 `Instant`로 변경하고, 테스트용 테이블의 `timestamp` 컬럼을 `timestamptz`로 변경 |

### 새 테스트

| 테스트 | 모듈 | 확인 내용 |
|---|---|---|
| `DdlScriptTest.timeColumnsAreTimestamptz` | member-domain | 시간대 없는 `timestamp` 컬럼이 없고, `timestamptz` 컬럼이 38개이며 `member.created_at`, `member_suspension.suspended_to`가 포함되는지 확인 |
| `FlywayTimestamptzMigrationTest` | member-api | Testcontainers에서 V1까지만 적용해 `timestamp '2026-10-14 09:00:00'` 값을 저장한 뒤 V2를 적용하면, 컬럼이 `timestamp with time zone`으로 바뀌고 값이 UTC `2026-10-14T00:00:00Z`로 변환되는지 확인 |

- `FlywayTimestamptzMigrationTest`는 Flyway를 코드로 구성해 `target("1")`, `target("2")`로 버전을 나눠 적용한다.
- member-domain 테스트는 `MigrationScripts`가 `docs/flyway`의 V1, V2를 버전 순으로 적용하므로 별도 설정이 필요 없었다.

## 문서

- README.md에 V2 설명, 엔티티 시각 규칙(`Instant`, `timestamptz`, JSON은 UTC 기준 ISO 문자열, 달력 계산 시 `ZoneId`로 변환), 새 테스트를 반영했다.

## 검증

- `spotlessApply`를 실행해 포맷을 적용했다.
- `./gradlew clean test`로 전체 테스트 129개를 실행해 모두 통과했다. (기존 127개에 새 테스트 2개, 실패 0, 오류 0)

## 참고

- 로컬 DB는 V1까지만 적용된 상태이므로 앱을 다시 실행하면 Flyway가 V2만 자동으로 적용하고 기존 데이터를 서울 시간 기준으로 변환한다. (사용자가 직접 앱을 실행해 확인하는 부분은 이 작업에서 수행하지 않았다)
- 아직 커밋하지 않았다.

---
