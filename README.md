# choimory-dev-member-api

## 개요

- Choimory 개발 회원 API 프로젝트다.
- Java 21 기반 Spring Boot 회원 서버이며 Gradle 멀티 모듈로 구성한다.
- 현재는 회원 도메인의 DB 설계(DDL)와 엔티티, DTO, enum, `member-api`의 도메인별 API 기본 구조(클래스와 필드만, 메소드 없음), 공통 응답과 예외 핸들러, Flyway 마이그레이션까지 구현된 상태이며, API 메소드는 이후 구현한다.
- 작업 히스토리는 `.agents/histories`를 참고한다. (DB 설계와 구현: `.agents/histories/2026/02/17/modeling`, API 기본 구조와 공통 응답, 예외: `.agents/histories/2026/10/14/api-base-structure`, Flyway 적용: `.agents/histories/2026/10/14/flyway-apply`)

## 기술 구성

- Language
  - Java 21
- Build
  - Gradle Groovy DSL, Gradle Wrapper
  - Palantir Java Format, Spotless
- Framework
  - Spring Boot 3.5.x, Spring Data JPA
  - Spring Web, Spring Security, Spring Validation (member-api)
  - Spring Data Elasticsearch, Spring Data Redis (member-api, 의존성만 있고 설정은 이후 추가)
- Library
  - Hibernate, UUID Creator, Lombok, PostgreSQL Driver
- Test
  - JUnit5, Spring Boot Test, Testcontainers PostgreSQL 16

## 프로젝트 구조

```text
.
├── build.gradle                  # 공통 플러그인, Java 21 toolchain, Spotless 설정
├── settings.gradle               # member-domain, member-api
├── docs
│   ├── api/v1
│   ├── db/v1                     # 참고용 DDL, DML (앱과 테스트에서 사용하지 않는다)
│   │   ├── v1-choimory-dev-member-ddl.sql   # 테이블 DDL (12개 테이블)
│   │   └── v1-choimory-dev-member-dml.sql   # 초기 데이터 (현재 비어 있음)
│   └── flyway/v1                 # Flyway 마이그레이션 원본
│       └── V1__init_schema.sql   # 테이블 DDL (12개 테이블, COMMENT는 테이블별로 포함)
├── member-domain                 # 엔티티, DTO, enum (API 모듈과 분리된 도메인 모듈)
│   └── src/main/java/dev/choimory/member/domain
│       ├── common/command/entity     # BaseEntity
│       ├── common/command/response   # CommonResponse, CommonValidateResult
│       ├── member/command/{entity,dto,type}
│       ├── authority/command/{entity,dto,type}
│       └── agreement/command/{entity,dto}
└── member-api                    # Spring Boot 애플리케이션
    └── src/main
        ├── java/dev/choimory/member/api
        │   ├── ChoimoryDevMemberApiApplication.java
        │   ├── config/JpaConfig.java
        │   ├── common/exception          # CommonException, CommonExceptionHandler
        │   └── {member,authority,agreement,menu}/v1/{command,query}/{controller,service,handler,repository}
        └── resources
            ├── application.yml, application-datasource.yml, application-flyway.yml
            └── application-{local,dev,prod}.yml
```

- `member-api`는 `member-domain`에 의존하며, `JpaConfig`가 엔티티는 `dev.choimory.member.domain`, 레포지토리는 `dev.choimory.member.api` 패키지에서 스캔한다.
- 빌드 시 `docs/flyway`가 `member-api`의 classpath `db/migration/` 아래로 복사된다. (`processResources`, 결과: `db/migration/v1/V1__init_schema.sql`)

## 도메인 모델

테이블은 엔티티 이름의 snake_case를 따르며(`@Table` 미사용), 모든 테이블에 공통 컬럼 `id, created_at, modified_at, deleted_at`이 있다.

| 영역 | 엔티티 | 설명 |
|---|---|---|
| 회원 | Member | 회원. 이미지, 정지 이력, 권한, 약관 동의 이력을 소유 |
| | MemberImage | 회원 이미지(원본과 리사이즈 정보) |
| | MemberSuspension | 회원 정지 이력 |
| | Follow | 팔로우. 자기 팔로우는 DB CHECK로 막고, hard delete하며 컬렉션으로 매핑하지 않음 |
| 권한 | Authority | 권한. 메뉴 접근 정보(Permission)를 소유 |
| | Menu | 메뉴. 부모 메뉴를 가리키는 자기참조 |
| | Permission, PermissionAction | 권한별 메뉴 접근 정보와 허용 액션(CREATE, READ, UPDATE, DELETE) |
| | MemberAuthority | 회원에 부여된 권한 |
| 약관 | Agreement | 약관 |
| | AgreementAnswer | 약관의 선택 항목 |
| | MemberAgreement | 회원이 선택한 약관 항목 이력 |

- enum: `MemberStatus`(REGISTER, ACTIVATE, INACTIVATE, SUSPENDED), `MemberImageType`(PROFILE), `AuthorityCode`(MEMBER, ADMIN, MODERATOR), `PermissionActionType`
- enum 컬럼은 문자열(varchar)로 저장한다.

## 엔티티 규칙

- id
  - Java `String`, DB `varchar(36)`, UUID v7을 `BaseEntity` 생성자에서 채번한다.
  - `Persistable<String>`과 `@Transient` 신규 여부 플래그(`@PostLoad`, `@PostPersist`에서 false)로 미리 채번된 id로 `save()`해도 SELECT가 나가지 않는다.
- Auditing
  - `@EnableJpaAuditing(modifyOnCreate = false)`이므로 `created_at`만 채우고 `modified_at`은 첫 수정 전까지 null이다.
- Soft delete
  - 각 엔티티에 `@SQLRestriction("deleted_at IS NULL")`을 직접 선언한다. (`BaseEntity`에서 상속되지 않는다. `EntityRulesTest`가 누락을 검사한다)
  - 삭제된 행을 조회해야 하면 네이티브 쿼리(또는 QueryDSL 필터)를 사용한다.
  - 삭제는 `softDelete()`로 한다. Follow는 hard delete한다.
- 생성
  - 정적 팩토리 없이 클래스 레벨 `@Builder`로 생성한다. (`@NoArgsConstructor(PROTECTED)`, `@AllArgsConstructor(PRIVATE)`, 컬렉션 필드는 `@Builder.Default`)
  - 부모 컬렉션에 등록하려면 `member.addImage(MemberImage.builder().member(member)...build())`처럼 부모의 `add*` 메소드를 호출한다. (등록한 자식을 반환한다)
  - 자기 팔로우 같은 비즈니스 검증은 엔티티가 아니라 Service에서 한다.
- cascade와 FK
  - 소유하는 자식은 `CascadeType.ALL`(orphanRemoval 없음), DB FK는 CASCADE
  - 참조만 하는 관계는 cascade 없음, DB FK는 RESTRICT (menu 부모, agreement_answer, authority, menu)
  - Menu 하위 메뉴는 PERSIST, MERGE만 전파한다.
- 수정은 엔티티 메소드(`updateProfile`, `changePassword`, `changeStatus` 등)로 한다.

## DTO 규칙

- Java `record`와 `@Builder(toBuilder = true)`를 사용하고, 정적 `from(Entity)`로 변환한다. (Entity에는 DTO 변환 메소드를 두지 않는다)
- 자식 DTO에는 부모 객체와 부모 id를 두지 않는다.
- 참조만 하는 관계는 `fromWithoutChildren`으로 변환해 하위 컬렉션을 건드리지 않는다.
- 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 변환한다.

## API 구조

- 도메인은 `member`, `authority`, `agreement`, `menu` 4개이며, 패키지는 `dev.choimory.member.api.{domain}.v1.{command|query}.{controller|service|handler|repository}`다.
- 클래스 이름은 Controller, Service, Handler가 `{Domain}{Command|Query}{Layer}`(예: `MenuQueryController`), Repository가 `{Domain}Repository`다. command와 query의 Handler 이름이 같으면 Spring 빈 이름이 충돌하므로 Command, Query를 구분해 붙인다.
- 요청 주소는 Controller 클래스의 `@RequestMapping("/v1/{domain}")`이며, command와 query Controller가 같은 기본 주소를 쓴다.
- 주입 관계는 Controller → Service → Handler → Repository이며 `@RequiredArgsConstructor`와 `private final` 필드로 생성자 주입한다. Handler는 자기 도메인의 Repository만 주입받는다.
- command Repository는 루트 엔티티(Member, Authority, Agreement, Menu)마다 `JpaRepository<엔티티, String>`으로 두고, 필요하면 추가한다.
- query Repository 패키지는 비어 있으며(`.gitkeep`) Elasticsearch 조회로 구현할 예정이다. query Handler는 Repository 없이 클래스만 있다.
- 현재는 클래스와 필드만 있고 메소드는 없다.

## 공통 응답과 예외

- `CommonResponse<T>`(`code`, `name`, `message`, `data`)와 `CommonValidateResult`(`field`, `rejectValue`, `message`)는 `member-domain`의 `common/command/response`에 record로 있다. (`command/dto`는 Entity와 1:1 DTO 전용이라 `DtoRulesTest`가 검사한다)
- `CommonException`(HTTP 상태, 응답 코드, 코드명, 메시지)과 `CommonExceptionHandler`는 `member-api`의 `common.exception`에 있다.
- `CommonExceptionHandler`는 `@RestControllerAdvice("dev.choimory")`이며 다음을 처리한다.
  - `CommonException`: 예외에 지정된 상태와 값
  - `MethodArgumentNotValidException`, `ConstraintViolationException`: 400, `data`에 `CommonValidateResult` 목록
  - `AuthenticationException`: 401, `data`에 예외 메시지
  - 그 외 `Exception`: 500, 예외는 로그로만 남기고 응답에 메시지를 담지 않는다.

## DB와 실행 환경

- PostgreSQL을 사용한다. 연결 정보는 `application-datasource.yml`의 환경변수(`rdb-host`, `rdb-port`, `rdb-database`, `rdb-user`, `rdb-password`)로 받는다.
- profile: 기본은 prod이며, `prod`, `dev`, `local` 프로필 그룹은 모두 `flyway` 프로필을 포함한다. (`application-prod.yml`은 `prod` 프로필에서만 적용되고 SQL 로그를 출력하지 않는다)
- 스키마는 Flyway가 앱 기동 시 `db/migration`의 마이그레이션 SQL로 관리한다. (`ddl-auto: none`, 설정은 `application-flyway.yml`: `validate-on-migrate: true`, `clean-disabled: true`)
  - 마이그레이션 원본은 `docs/flyway/v1`이다. 파일명은 `V{버전}__{설명}.sql`(밑줄 두 개)이며 버전 순으로 적용된다.
  - 이미 적용된 버전은 수정하지 않는다. (체크섬 검증으로 기동이 실패한다) 변경은 `V2__...sql`처럼 새 파일을 추가한다.
  - `V1__init_schema.sql`은 기존 DDL이 적용된 DB에서도 안전하도록 `create ... if not exists`를 유지하고 DROP 문은 쓰지 않는다. COMMENT는 각 테이블의 DDL 바로 아래에 둔다.
  - DML(초기 데이터)은 아직 없으며 필요할 때 `V2__...sql`로 추가한다.
  - `docs/db/v1`은 참고용이며 앱과 테스트에서 사용하지 않는다. 최신 스키마는 `docs/flyway` 기준이다.
- 기존 DDL이 적용된 DB(이력 테이블이 없는 비어 있지 않은 스키마)에 처음 붙이면 `Found non-empty schema(s) "public" but no schema history table` 오류로 기동하지 않는다. 로컬은 DB를 비우고 다시 만들고, 그 외 환경은 baseline 전략(`baseline-on-migrate`, `baseline-version`)을 정해 적용한다. (현재 설정에는 baseline이 없다)
- Flyway는 `member-api`에만 적용하며 `member-domain`에는 의존성이 없다. 서비스별로 PostgreSQL database를 따로 두고 `public` 스키마를 쓰는 구성을 기본으로 한다.

## 코드 규칙

- DTO, Request, Response는 Java record와 `@Builder(toBuilder = true)`를 사용해 불변 객체로 작성한다.
- 데이터 객체의 정적 팩토리 메소드 내부 생성은 Builder 패턴을 사용한다.
- Controller는 하나의 Service 함수만 호출한다. Service는 API 흐름을, Handler는 세부 로직을 담당한다.
- Java 객체와 필드, 주요 함수에는 한글 Doc Comment를 작성한다.
- Java 코드 포맷은 Palantir Java Format을 사용한다. Gradle Daemon JDK가 Java 21이 아닐 수 있으므로 `gradle.properties`의 `palantir.native.formatter=true`로 native formatter를 쓴다.

## 테스트

- 테스트는 Testcontainers Postgres 16을 사용하며 Docker가 필요하다.
- `member-domain` 테스트는 `docs/flyway`의 `V*.sql`을 버전 순으로 컨테이너 초기화 스크립트에 적용하고 `ddl-auto=validate`로 엔티티와 스키마 정합성을 검증한다. (gradle이 시스템 프로퍼티 `docs.flyway.dir`로 경로를 전달하고, 테스트의 `MigrationScripts`가 버전 숫자 순으로 정렬한다)
- `member-api` 테스트도 Testcontainers Postgres 16을 쓰며, 앱 기동 시 Flyway가 실제로 마이그레이션을 적용한다.
- 주요 테스트
  - `DdlScriptTest`: unique, check, FK, 마이그레이션 SQL 재실행(멱등)
  - `MigrationScriptsTest`: 마이그레이션 파일명 규칙, 버전 순 정렬
  - `FlywayMigrationTest`(member-api): Flyway 이력에 V1 성공 기록, 12개 테이블 생성, `ddl-auto=validate`로 엔티티와 스키마 정합성
  - `FlywayProfileTest`(member-api): local, dev, prod 프로필의 flyway 활성화, 마이그레이션 SQL의 classpath 복사, prod 프로필 설정
  - `BaseEntityTest`, `MemberEntitiesTest`, `AuthorityEntitiesTest`, `AgreementEntitiesTest`: 저장, 조회, cascade, soft delete
  - `EntityRulesTest`: 모든 엔티티의 `@SQLRestriction` 선언, 클래스 레벨 Builder, `@Table` 미사용, 정적 `create` 없음
  - `DtoRulesTest`, `*DtosTest`: DTO 규칙과 변환
  - `EntityMappingTest`: 엔티티 매핑과 DDL 컬럼 일치
  - `EnumValuesTest`: enum 값과 DDL check 일치
  - `CommonResponseTest`, `CommonValidateResultTest`: 공통 응답 객체
  - `CommonExceptionTest`, `CommonExceptionHandlerTest`(member-api): 공통 예외와 예외별 응답. 예외를 던지는 테스트용 컨트롤러와 standalone MockMvc를 사용하며 Docker가 필요 없다.
- JPA 테스트는 `@JpaEntityTest`를 사용하며, Auditing은 `JpaAuditingTestConfiguration`을 import한 테스트에서만 켠다. (테스트 앱에 `@EnableJpaAuditing`을 두면 `@JdbcTest`가 깨진다)

## 검증과 코드 포맷

- 전체 빌드와 테스트: `./gradlew clean build`
- 모듈별 테스트: `./gradlew :member-domain:test`, `./gradlew :member-api:test`
- 포맷 검사: `./gradlew spotlessCheck`
- 포맷 적용: `./gradlew spotlessApply`
