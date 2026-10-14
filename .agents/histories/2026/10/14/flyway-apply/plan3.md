# 목차

- [개요](#개요)
- [Testcontainers 사용 방법](#testcontainers-사용-방법)
- [Testcontainers 컨테이너의 실행 위치와 정리](#testcontainers-컨테이너의-실행-위치와-정리)
- [Testcontainers 테스트 실행 방식](#testcontainers-테스트-실행-방식)
- [Testcontainers DB 컨테이너 설정의 기준](#testcontainers-db-컨테이너-설정의-기준)
- [Flyway가 없는 프로젝트의 스키마 구성](#flyway가-없는-프로젝트의-스키마-구성)
- [timestamp와 timestamptz](#timestamp와-timestamptz)
- [timestamptz 변경 계획](#timestamptz-변경-계획)
- [Instant를 권하는 이유](#instant를-권하는-이유)

---

# 개요

-

---

# Testcontainers 사용 방법

> 이 프로젝트 testcontainer사용하려면 어떻게 함?

## 답변

- 이 프로젝트에는 Testcontainers가 이미 설정되어 있어서, Docker만 켜져 있으면 추가 설정 없이 테스트에서 Postgres 컨테이너가 자동으로 뜬다.

## 사용 조건

- Docker가 실행 중이어야 한다. (확인 시점에 Docker 28.2.2가 켜져 있었고 `~/.testcontainers.properties`도 있었다)
- 컨테이너 이미지는 `postgres:16`이며 처음 한 번만 받는다.

## 테스트 실행

- 전체: `./gradlew clean test`
- 모듈별: `./gradlew :member-domain:test`, `./gradlew :member-api:test`
- 한 클래스만: `./gradlew :member-api:test --tests '*FlywayMigrationTest'`
- IntelliJ에서는 테스트 클래스나 메소드의 실행 버튼을 누른다.

## 구조

| 모듈 | 설정 클래스 | 동작 |
|---|---|---|
| `member-api` | `TestcontainersConfiguration` | `postgres:16` 컨테이너를 만들고, `@ServiceConnection`이 접속 정보(URL, 계정)를 Spring에 자동 주입한다. 앱이 뜨면 Flyway가 `docs/flyway`의 SQL을 적용한다. |
| `member-domain` | `TestcontainersConfiguration` | 같은 컨테이너에 `docs/flyway`의 `V*.sql`을 버전 순으로 `/docker-entrypoint-initdb.d`에 복사해 초기화한다. Flyway는 쓰지 않는다. |

## 새 테스트에서 쓰는 방법

- 컨테이너가 필요한 테스트 클래스에 `@Import(TestcontainersConfiguration.class)`를 붙인다.
  - `member-api`: `@SpringBootTest`와 함께 사용한다. (예: `FlywayMigrationTest`)
  - `member-domain`: `@JpaEntityTest`가 이미 `TestcontainersConfiguration`을 import 한다. `@JdbcTest`로 직접 쓸 때는 `@AutoConfigureTestDatabase(replace = NONE)`와 함께 import 한다.
- 컨테이너가 필요 없는 단위 테스트는 어노테이션 없이 일반 JUnit 테스트로 작성한다. `CommonExceptionHandlerTest`는 standalone MockMvc라 Docker가 필요 없다.

## 앱을 Testcontainers 컨테이너로 로컬 실행

- `TestChoimoryDevMemberApiApplication`의 `main`을 실행하면, 로컬 DB 없이 Postgres 컨테이너가 자동으로 뜨고 그 DB로 앱이 실행된다.
- 일반 `ChoimoryDevMemberApiApplication`은 `application-datasource.yml`의 접속 정보(`rdb-host` 등)로 실제 로컬 DB에 붙는다.
- 컨테이너로 띄운 앱은 종료하면 DB도 사라진다. 데이터를 유지하려면 로컬 DB를 쓰는 쪽을 사용한다.

## 자주 만나는 문제

- `Could not find a valid Docker environment`: Docker Desktop이 꺼져 있거나 소켓 경로가 다른 경우다. Docker를 켜고 `docker ps`가 되는지 확인한다.
- 느린 첫 실행: 이미지를 처음 받을 때이며 이후에는 캐시된다.
- 설정이 다른 컨텍스트마다 컨테이너가 따로 뜰 수 있어 `@SpringBootTest`가 많아지면 느려진다. 필요하면 컨테이너를 싱글톤으로 재사용하는 구성을 검토한다. (지금은 필요 없다)

---

# Testcontainers 컨테이너의 실행 위치와 정리

> testcontainer가 테스트를 도커내에서 실행하고 끝나면 도커 컨테이너 다시 죽이는거 맞지?

## 답변

- 거의 맞지만 "테스트를 도커 안에서 실행한다"는 부분은 다르다.

## 정확한 동작

- 테스트 코드는 호스트(내 PC의 JVM)에서 실행된다. Docker 안에서 도는 것은 Postgres 컨테이너뿐이다.
- 테스트가 시작되면 Testcontainers가 `postgres:16` 컨테이너를 띄우고, 테스트 JVM이 그 컨테이너의 포트에 접속한다. 포트는 호스트의 랜덤 포트로 매핑되고 `@ServiceConnection`이 접속 정보를 자동으로 주입한다.
- 테스트가 끝나면(JVM이 종료되거나 컨텍스트가 닫히면) 컨테이너가 정리된다.

## 정리 방식

- Testcontainers는 `Ryuk`이라는 보조 컨테이너를 함께 띄운다. 테스트 JVM과의 연결이 끊기면 Ryuk이 해당 컨테이너와 네트워크를 지운다. 정상 종료뿐 아니라 JVM이 강제로 죽었을 때도 대부분 정리된다.
- IntelliJ에서 강제 중단하면 컨테이너가 드물게 남을 수 있다. 그럴 때는 `docker ps`로 `postgres:16`과 `testcontainers/ryuk` 컨테이너를 확인하고 `docker rm -f`로 지운다.

## 이 프로젝트에서 알아 둘 점

- 컨테이너가 테스트 클래스마다 새로 뜨는 것은 아니다. Spring이 같은 설정의 컨텍스트를 재사용하므로 같은 컨테이너를 쓰고, 설정이 다른 컨텍스트(예: `@ActiveProfiles`가 다른 경우)는 컨테이너가 각각 뜬다.
- 컨테이너 데이터는 테스트 종료와 함께 사라지므로 DB 상태는 테스트 사이에 남지 않는다.
- `TestChoimoryDevMemberApiApplication`으로 앱을 실행한 경우도 같은 방식이라 앱을 종료하면 DB 컨테이너가 정리된다.

---

# Testcontainers 테스트 실행 방식

> 아 그럼 테스트 실행은 기존 Springboot 테스트 돌리듯이 돌리면 되는거고?

## 답변

- 맞다. 일반 Spring Boot 테스트와 같은 방식으로 실행하면 되고, 달라지는 것은 Docker가 켜져 있어야 한다는 점뿐이다.
  - Gradle: `./gradlew test`, `./gradlew :member-api:test` 등 기존과 같다.
  - IntelliJ: 테스트 클래스나 메소드 옆 실행 버튼을 누른다.
  - 테스트 코드: 컨테이너가 필요한 테스트에는 `@Import(TestcontainersConfiguration.class)`가 이미 붙어 있다. 컨테이너를 직접 띄우거나 끄는 코드는 필요 없다.
  - Docker: 켜져 있으면 컨테이너 생성과 정리는 Testcontainers가 알아서 한다. 꺼져 있으면 `Could not find a valid Docker environment` 오류가 난다.
- Docker가 필요 없는 테스트도 있다. `CommonExceptionHandlerTest`, `CommonResponseTest`, `MigrationScriptsTest`처럼 DB를 쓰지 않는 단위 테스트는 Docker 없이 돌아간다. 전체를 돌리면 DB를 쓰는 테스트가 Docker 때문에 실패할 수 있다.

---

# Testcontainers DB 컨테이너 설정의 기준

> testcontainer로 만들어지는 db 컨테이너의 셋팅은 무엇을 기반으로함? entity?

## 답변

- 엔티티 기반이 아니다. 컨테이너의 DB 구조는 `docs/flyway`의 마이그레이션 SQL(DDL)로 만들어지고, 엔티티는 그 구조와 맞는지 검증하는 데만 쓰인다.

## 컨테이너 설정의 출처

| 항목 | 기준 |
|---|---|
| 이미지 | `TestcontainersConfiguration`에 적힌 `postgres:16` |
| DB 이름, 계정, 비밀번호 | Testcontainers 기본값(`test` / `test` / `test`). `@ServiceConnection`이 접속 정보를 Spring에 자동으로 넘기므로 `application-datasource.yml`의 값은 쓰이지 않는다. |
| 포트 | 호스트의 랜덤 포트로 매핑된다. |
| 테이블과 스키마 | `docs/flyway/v1/V1__init_schema.sql` 등의 마이그레이션 SQL |

## 모듈별로 스키마가 만들어지는 방식

- `member-api`: 앱이 뜰 때 Flyway가 `db/migration`의 SQL을 컨테이너 DB에 적용한다. `docs/flyway`가 빌드 시 classpath로 복사된 것이다.
- `member-domain`: Flyway를 쓰지 않는다. `TestcontainersConfiguration`이 `docs/flyway`의 `V*.sql`을 버전 순으로 컨테이너의 `/docker-entrypoint-initdb.d`에 복사하고, Postgres가 컨테이너를 처음 띄울 때 그 스크립트를 실행한다.

## 엔티티의 역할

- `ddl-auto=validate`로 실행하므로, Hibernate가 엔티티 매핑과 컨테이너 DB의 테이블 구조를 비교해 어긋나면 컨텍스트가 뜨지 않고 테스트가 실패한다. 엔티티가 스키마를 만들지는 않는다.
- `ddl-auto=create` 같은 설정은 쓰지 않으므로 엔티티를 고쳐도 DB 구조는 바뀌지 않는다. 컬럼을 추가하려면 새 마이그레이션 파일(`V2__...sql`)을 먼저 만들어야 한다.
- 테스트가 통과하면 "엔티티와 실제 마이그레이션이 일치한다"는 뜻이다. 엔티티로 스키마를 자동 생성하는 것보다 신뢰도가 높다.

---

# Flyway가 없는 프로젝트의 스키마 구성

> flyway가 없는 프로젝트는 보통 뭘 기반으로 만들어져?

## 답변

- Flyway나 Liquibase 같은 마이그레이션 도구가 없는 프로젝트가 테스트 DB 스키마를 만드는 방식은 크게 네 가지이며, 프로젝트마다 다르다.

## 1. 엔티티로 자동 생성 (`ddl-auto=create` 또는 `create-drop`)

- 가장 흔한 방식이다. Hibernate가 엔티티를 읽어 테이블을 만든다. 설정이 필요 없고 엔티티를 고치면 DB 구조가 바로 따라오며, 인메모리 DB(H2)와 함께 쓰는 경우가 많다.
- 단점은 테스트 스키마가 운영 DDL과 달라질 수 있다는 점이다. 인덱스, 제약조건, FK 삭제 동작, 컬럼 COMMENT, `check` 제약은 엔티티로 표현이 안 되거나 다르게 만들어진다. 테스트가 통과해도 운영 DDL과 어긋날 수 있다.

## 2. 스키마 SQL 파일 (`schema.sql` 또는 sql-init)

- Spring의 `spring.sql.init`이 `schema.sql`, `data.sql`을 앱이 뜰 때 실행한다. 이 프로젝트가 Flyway 이전에 `local`에서 쓰던 방식이다.
- 운영 DDL과 같은 SQL을 쓸 수 있다는 장점이 있다.
- 단점은 버전 관리와 이력이 없다는 점이다. 매번 전체를 실행하므로 `if not exists` 같은 멱등한 작성이 필요하고, 변경 이력을 DB에 남기지 못한다.

## 3. Testcontainers에 SQL 스크립트를 직접 연결

- 이 프로젝트의 `member-domain` 테스트 방식이다. `withInitScript` 또는 `/docker-entrypoint-initdb.d`로 DDL 파일을 컨테이너에 넣는다.
- 운영과 같은 DDL을 쓰면서 Flyway 의존성도 필요 없다.
- 단점은 파일이 여러 개가 되면 순서와 적용 이력을 직접 관리해야 한다는 점이다.

## 4. 운영 DB 덤프를 사용

- 운영 DB의 구조(`pg_dump --schema-only`)를 뽑아 테스트에 쓰는 방식이다. 레거시 프로젝트에서 종종 보인다.
- 실제 구조와 같다는 장점이 있지만, 덤프가 낡으면 이를 갱신하는 작업이 별도로 필요하다.

## 비교

| 방식 | 운영 DDL과 일치 | 이력 관리 | 설정 난이도 |
|---|---|---|---|
| 엔티티 자동 생성 | 낮음 | 없음 | 가장 쉬움 |
| `schema.sql` (sql-init) | 높음 | 없음 | 쉬움 |
| Testcontainers + SQL 파일 | 높음 | 직접 관리 | 보통 |
| 마이그레이션 도구(Flyway 등) | 높음 | DB에 기록 | 보통 |
| 운영 덤프 | 높음 | 직접 관리 | 보통 |

## 정리

- 개인 프로젝트나 프로토타입은 엔티티 자동 생성으로 시작하는 경우가 가장 많다.
- 운영 DB가 있는 프로젝트에서는 스키마 변경 이력이 필요해서 마이그레이션 도구로 옮기는 흐름이 일반적이다.
- 이 프로젝트는 처음부터 DDL을 원본으로 두고(`docs/db`), sql-init에서 Flyway로 발전한 경우다.

---

# timestamp와 timestamptz

> 근데 postgres는 datetime이 없어? 아니면 있는데 timestamp가 더 좋은거야?

## 답변

- Postgres에는 `datetime` 타입이 없다. MySQL의 `datetime`에 해당하는 것으로 `timestamp`와 `timestamptz`가 있고, 일반적으로는 `timestamptz`가 더 권장된다.

## Postgres의 날짜/시간 타입

| 타입 | 의미 | 저장 크기 |
|---|---|---|
| `timestamp` (= `timestamp without time zone`) | 시간대 정보 없는 "그냥 날짜+시각" 값. 넣은 값 그대로 저장하고 꺼낸다. | 8바이트 |
| `timestamptz` (= `timestamp with time zone`) | 시간대를 반영하는 시각. 내부적으로 UTC로 저장하고, 조회할 때 세션의 시간대로 변환해서 보여 준다. | 8바이트 |

- 이름과 달리 `timestamptz`가 시간대 자체를 저장하는 것은 아니다. 입력을 UTC로 정규화해서 저장하고 출력할 때 세션 시간대로 바꾼다.

## 어느 쪽이 좋은가

- PostgreSQL 위키는 `timestamp without time zone` 사용을 피하라고 권한다. 같은 시각이 어느 시간대로 해석되는지가 명확하지 않기 때문이다.
- `timestamptz`가 안전한 경우: 서버, 클라이언트, DB가 다른 시간대에 걸쳐 있거나 해외 사용자가 있거나 서머타임이 있는 경우다. 시각이 "순간"을 나타내는 값(생성 일시, 로그인 시각 등)이라면 거의 항상 `timestamptz`가 맞다.
- `timestamp`도 괜찮은 경우: 시각이 "벽시계 값"이라 시간대가 의미 없는 경우다. 예를 들면 "매일 오전 9시 예약" 같은 값이다. 또는 앱 서버 시간대가 항상 같고 변환이 필요 없는 경우다.

## 이 프로젝트의 현재 상태

- DDL의 `created_at`, `modified_at`, `deleted_at` 등은 모두 `timestamp`(시간대 없음)이다.
- 엔티티는 `LocalDateTime`을 쓸 것으로 보이며, `LocalDateTime`과 `timestamp`는 자연스럽게 맞는다.
- 서버 시간대가 바뀌면 같은 값이 다른 시각으로 해석된다. 예를 들어 서버가 KST인 환경에서 저장한 값을 UTC 서버가 읽으면 9시간이 어긋난다. 서버를 하나의 시간대로 고정해 운영한다면 문제가 없다.

## 바꾸려면

- DDL의 `timestamp`를 `timestamptz`로 바꾸고, 엔티티를 `OffsetDateTime` 또는 `Instant`로 바꾸는 방식이 일반적이다. (Hibernate는 `Instant`와 `OffsetDateTime`을 `timestamptz`로 매핑한다)
- 이 변경은 DTO, 응답 객체, 테스트까지 영향을 준다.
- 이미 V1을 적용한 DB가 있다면 V1을 수정할 수 없으므로 `V2__...sql`로 `alter table ... alter column ... type timestamptz`를 추가해야 한다. 로컬만 쓰는 지금이라면 V1을 직접 고쳐서 DB를 다시 만드는 쪽이 간단하다.

## 결정이 필요한 것

- `timestamptz`로 바꿀지 여부는 아직 정하지 않았다. 지금은 아무것도 수정하지 않았다.
- 서비스가 해외 사용자나 여러 서버 시간대를 다룰 가능성이 있다면 `timestamptz`를 권한다.

---

# timestamptz 변경 계획

> timestamptz로 변경하자. docs/db의 sql 수정하고, flyway도 함 써먹어보자

## 답변

- 변경 범위를 확인했고, 아직 수정한 것은 없다. 정해야 할 것을 정리했다.

## 변경 대상

- DDL: `timestamp` 컬럼은 38개다. 테이블 12개의 `created_at`, `modified_at`, `deleted_at` 36개와 `member_suspension`의 `suspended_at`, `suspended_to` 2개다.
- 엔티티: `BaseEntity`(`createdAt`, `modifiedAt`, `deletedAt`, `softDelete()`의 `LocalDateTime.now()`)와 `MemberSuspension`(`suspendedAt`, `suspendedTo`)
- DTO: `LocalDateTime`을 쓰는 DTO 12개
- 테스트: `LocalDateTime`을 쓰는 3개 파일(`MemberDtosTest`, `BaseEntityTest`, `MemberEntitiesTest`)을 고쳐야 한다. `DdlScriptTest`의 `now()` insert와 `EntityMappingTest`는 영향 여부를 구현하면서 확인한다.

## 계획

### DB (Flyway를 실제로 써 보는 부분)

- `V1__init_schema.sql`은 수정하지 않는다. V1은 이미 로컬 DB에 적용되어 있고, 수정하면 체크섬 오류로 기동이 실패한다.
- 대신 `docs/flyway/v1/V2__timestamp_to_timestamptz.sql`을 새로 만든다. 컬럼 38개를 `alter table ... alter column ... type timestamptz using ... at time zone '...'`로 바꾼다.
- 이 변경으로 로컬 DB에서는 앱이 뜰 때 Flyway가 V2만 새로 적용한다. member-domain 테스트는 V1과 V2를 순서대로 적용한다.
- `COMMENT`는 타입과 무관해서 V2에 넣지 않는다.
- `docs/db/v1`의 참고용 DDL은 직접 `timestamptz`로 바꾼다.

### 엔티티와 DTO

- `LocalDateTime`을 `Instant`로 바꾸는 것을 권한다.
  - Hibernate 6는 `Instant`를 `timestamptz`로 매핑하고, Spring Data 감사(`@CreatedDate`)도 `Instant`를 지원한다.
  - JSON으로는 UTC 기준 ISO 문자열(`...Z`)로 나가서 해석이 모호하지 않다.
  - `OffsetDateTime`은 오프셋을 같이 들고 다니지만, 이 프로젝트에서는 얻는 것이 적고 비교와 테스트가 번거롭다.
- `softDelete()`는 `Instant.now()`를 쓴다.

## 정해야 할 것

1. 타입: `Instant`(추천)로 갈지, `OffsetDateTime`으로 갈지
2. V2의 변환 기준 시간대: 기존 `timestamp` 값은 앱이 서버 시간대(보통 KST)로 쓴 값이다. `using ... at time zone 'Asia/Seoul'`로 고정하면 기존 값이 올바른 시각으로 변환된다. 마이그레이션은 한 번 적용되면 고칠 수 없어서 시간대를 명시하는 것이 안전하다. `Asia/Seoul`로 해도 되는지
3. 작업 디렉토리: 별도 디렉토리(예: `timestamptz-migration`)를 만들고 PLAN부터 시작할지, 이미 DONE인 `flyway-apply`를 이어서 쓸지(별도 디렉토리를 권한다)
4. 시작 여부: 위 내용을 확정하고 시작해도 되는지

---

# Instant를 권하는 이유

> instant로 해야하는 이유는 뭐야?

## 답변

- `timestamptz`가 저장하는 값의 성질과 `Instant`의 성질이 가장 정확하게 일치하기 때문이다.

## 핵심 이유

- `timestamptz`는 "시간대가 정규화된 하나의 순간"만 저장한다. 내부적으로 UTC로 저장하고, 입력 때 쓴 오프셋(`+09:00` 같은 정보)은 보관하지 않는다.
- `Instant`는 타임라인 위의 한 순간을 나타내는 타입이라 의미가 그대로 맞는다.
- `OffsetDateTime`은 오프셋을 함께 들고 다니는 타입인데, DB가 오프셋을 보관하지 않으므로 다시 읽으면 입력 때의 오프셋이 아니라 드라이버나 설정에 따라 다른 오프셋(UTC나 세션 시간대)으로 돌아온다. 코드가 "오프셋이 유지된다"고 오해하기 쉽다.

## 타입 비교

| | `Instant` | `OffsetDateTime` | `LocalDateTime` (현재) |
|---|---|---|---|
| `timestamptz`와의 의미 일치 | 정확히 일치 | 오프셋 정보가 왕복되지 않음 | 시간대가 없어 의미가 다름 |
| 현재 시각 | JVM 기본 시간대와 무관 | 오프셋을 정해야 함 | JVM 기본 시간대에 의존(지금 문제) |
| 비교와 `equals` | 같은 순간이면 같음 | 같은 순간이어도 오프셋이 다르면 `equals`가 false라서 테스트에서 `isEqual`을 써야 함 | 시간대 해석에 따라 달라짐 |
| Hibernate 6 매핑 | 기본으로 `timestamptz`로 매핑 | `hibernate.timezone.default_storage` 설정에 따라 달라짐 | `timestamp` |
| JSON 직렬화 | ISO 문자열 UTC(`2026-10-14T14:00:00Z`)로 모호함 없음 | 오프셋이 문자열에 들어가 서버 설정에 따라 값이 달라질 수 있음 | 시간대 없는 문자열이라 클라이언트가 해석해야 함 |
| Spring Data 감사(`@CreatedDate`) | 지원 | 지원 | 지원 |

## 단점

- 날짜 계산이 불편하다. `Instant`에는 `plusMonths` 같은 달력 연산이 제한적이다. (일 단위는 `plus(1, ChronoUnit.DAYS)`로 가능) "정지 종료일 = 한 달 뒤 같은 시각"처럼 달력 규칙이 필요하면 `ZoneId`를 붙여 `ZonedDateTime`으로 바꿔서 계산해야 한다.
- 사람에게 보여 줄 때는 변환이 필요하다. 화면이나 API 응답에서 지역 시간으로 보여 주려면 `atZone(...)`으로 변환한다. 다만 이것이 `timestamptz`의 권장 사용법이다. (저장은 UTC, 표시할 때 시간대 적용)

## 정리

- 이 프로젝트의 `created_at`, `modified_at`, `deleted_at` 같은 값은 모두 "발생한 순간"이라 `Instant`가 잘 맞는다.
- `suspended_at`, `suspended_to`도 순간이지만, 정지 기간을 달력 규칙(예: 한 달)으로 계산하는 로직이 생기면 그 부분에서 `ZoneId`를 붙여 `ZonedDateTime`으로 변환하면 된다.
- `OffsetDateTime`도 틀린 선택은 아니고, 오프셋이 함께 있는 것이 편한 팀도 있다. 위 비교 때문에 `Instant`를 권했을 뿐이다. `OffsetDateTime`을 선택하면 `hibernate.timezone.default_storage` 설정을 같이 정해야 한다.

---
