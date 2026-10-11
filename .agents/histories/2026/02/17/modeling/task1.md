# 목차

- [개요](#개요)
- [0단계. 시작 준비](#0단계-시작-준비)
- [1단계. Gradle 멀티 모듈 전환](#1단계-gradle-멀티-모듈-전환)
  - [변경 내용](#변경-내용)
  - [진행 중 발생한 문제와 해결](#진행-중-발생한-문제와-해결)
  - [검증 결과](#검증-결과)
  - [커밋 단위 제안](#커밋-단위-제안)
- [다음 작업](#다음-작업)
- [2단계. 패키지 골격, 스캔 설정, 설정 파일 정리](#2단계-패키지-골격-스캔-설정-설정-파일-정리)
  - [변경 내용](#변경-내용-1)
  - [TDD 진행 기록](#tdd-진행-기록)
  - [계획과 달라진 점](#계획과-달라진-점)
  - [검증 결과](#검증-결과-1)
  - [커밋 단위 제안](#커밋-단위-제안-1)
- [다음 작업 (3단계)](#다음-작업-3단계)
- [package-info.java 삭제](#package-infojava-삭제)
- [3단계. DDL 작성과 id 타입 검증](#3단계-ddl-작성과-id-타입-검증)
  - [id 타입 검증 결과](#id-타입-검증-결과)
  - [DDL 작성 내용](#ddl-작성-내용)
  - [테스트 인프라와 테스트](#테스트-인프라와-테스트)
  - [임의로 정한 부분](#임의로-정한-부분)
  - [검증 결과](#검증-결과-2)
  - [커밋 단위 제안](#커밋-단위-제안-2)
- [4단계. BaseEntity](#4단계-baseentity)
  - [구현 내용](#구현-내용)
  - [계획과 달라진 점](#계획과-달라진-점-1)
  - [테스트](#테스트)
  - [남은 선택 사항](#남은-선택-사항)
  - [검증 결과](#검증-결과-3)
  - [커밋 단위 제안](#커밋-단위-제안-3)
- [5단계. enum](#5단계-enum)
  - [구현 내용](#구현-내용-1)
  - [계획과 달라진 점](#계획과-달라진-점-2)
  - [테스트](#테스트-1)
  - [남은 선택 사항](#남은-선택-사항-1)
  - [검증 결과](#검증-결과-4)
  - [커밋 단위 제안](#커밋-단위-제안-4)
- [6단계. 엔티티](#6단계-엔티티)
  - [6단계 전에 결정된 사항](#6단계-전에-결정된-사항)
  - [구현한 엔티티](#구현한-엔티티)
  - [Builder 적용](#builder-적용)
  - [테스트](#테스트-2)
  - [임의로 정한 부분](#임의로-정한-부분-1)
  - [검증 결과](#검증-결과-5)
  - [커밋 단위 제안](#커밋-단위-제안-5)
- [6단계 후속 변경. 엔티티 생성 방식 정리](#6단계-후속-변경-엔티티-생성-방식-정리)
- [7단계. 엔티티 DTO](#7단계-엔티티-dto)
- [8단계. DDL과 엔티티 정합성 검증](#8단계-ddl과-엔티티-정합성-검증)
- [9단계. 최종 점검](#9단계-최종-점검)
- [엔티티 create() 제거와 클래스 레벨 Builder 전환](#엔티티-create-제거와-클래스-레벨-builder-전환)

---

# 개요

- plan4.md의 "최종 구현 계획"에 따라 구현을 진행한 작업 내용을 기록함
- 이 문서에는 0단계(시작 준비)와 1단계(Gradle 멀티 모듈 전환)를 기록함
- 2단계 이후는 진행하면서 이어서 기록함
- git commit과 push는 직접 진행하므로 수행하지 않았음

---

# 0단계. 시작 준비

## 한 일

- calendar.md에서 2026-02-17 modeling을 PLAN에서 TASK로 변경
- RedisConfigTest.java를 삭제하고 비어 있게 된 src/test/java/dev/choimory/member/api/config 디렉토리도 삭제 (삭제 전 코드에서 사라진 RedisConfig, MemberEntityDto를 참조하여 컴파일되지 않던 테스트)
- 빌드 기준선 확인

## 검증 결과

- compileJava, compileTestJava 통과
- Docker(28.2.2)가 실행 중이며 컨텍스트 로딩 테스트(contextLoads)가 Testcontainers Postgres로 통과

---

# 1단계. Gradle 멀티 모듈 전환

## 변경 내용

### settings.gradle

- member-domain, member-api를 포함

### 루트 build.gradle

- 플러그인(Spotless, Palantir Java Format, Spring Boot, Spring Dependency Management)은 apply false로 선언
- allprojects: 저장소(mavenCentral, repo.spring.io/snapshot) 설정
- subprojects: java, Spotless, Palantir 플러그인 적용, group/version, Java 21 toolchain, compileOnly이 annotationProcessor를 확장하는 설정, 테스트 JUnit 플랫폼 사용

### member-domain/build.gradle (신규)

- java-library, io.spring.dependency-management 적용, Spring Boot BOM 임포트 (Spring Boot 플러그인은 적용하지 않아 bootJar 없음)
- 의존성
  - api: spring-boot-starter-data-jpa (member-api로 전달됨)
  - implementation: uuid-creator 5.3.2
  - Lombok (compileOnly, annotationProcessor, 테스트용 포함)
  - 테스트: spring-boot-starter-test, spring-boot-testcontainers, Testcontainers junit-jupiter와 postgresql, postgresql 드라이버(testRuntimeOnly), junit-platform-launcher
- src/main/java, src/test/java 디렉토리 생성 (아직 소스 없음)

### member-api/build.gradle (신규)

- Spring Boot 플러그인, io.spring.dependency-management 적용
- 의존성은 기존 build.gradle의 것을 이동하고 project(':member-domain')을 추가
  - JPA, Security, Validation, Web, Elasticsearch, Redis 스타터, jackson-databind, Lombok, postgresql 런타임 드라이버, configuration-processor, 테스트 의존성
  - uuid-creator는 엔티티에서만 사용하므로 제외

### 소스 이동

- 기존 src를 member-api/src로 이동
  - src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java
  - src/main/resources/ (application*.yml, sql/)
  - src/test/java/dev/choimory/member/api/ (ChoimoryDevMemberApiApplicationTests, TestChoimoryDevMemberApiApplication, TestcontainersConfiguration)

## 진행 중 발생한 문제와 해결

1. git mv 실패
   - 원인: 삭제한 RedisConfigTest가 아직 인덱스에 남아 있어 원본 경로를 찾지 못함 (이동은 일어나지 않음)
   - 해결: 인덱스를 건드리지 않기 위해 일반 mv로 이동. git status에는 삭제(D)와 신규(??)로 보이지만 커밋 시 Git이 이름 변경으로 감지함
2. spotlessJavaCheck 실패 (`Could not resolve all files for configuration ':palantirJavaFormatNative'`)
   - 원인: Palantir 플러그인이 네이티브 포매터 바이너리를 루트 프로젝트의 저장소에서 받는데, 루트에 저장소 설정이 없었음
   - 해결: 저장소 설정을 subprojects에서 allprojects로 변경. 이전 실패 결과가 Spotless 작업 캐시에 남아 있어 `./gradlew :member-api:spotlessJava --rerun`으로 다시 실행한 뒤 통과
   - 확인: 임시 init 스크립트로 palantirJavaFormatNative 설정이 macOS arm64 네이티브 실행 파일로 해석됨을 확인 (임시 스크립트는 scratchpad에만 두었고 프로젝트에는 남기지 않음)
   - 주의: 루트에 저장소 설정이 없으면 같은 문제가 다시 생기므로 allprojects의 저장소 설정은 유지해야 함

## 검증 결과

- ./gradlew build 통과 (member-domain, member-api 모두 compile, spotlessCheck, test, bootJar/jar)
- member-api의 컨텍스트 로딩 테스트 1건 통과 (tests=1, failures=0, errors=0)
- member-domain은 소스가 없어 NO-SOURCE로 건너뜀

## 커밋 단위 제안

- 1단계 전체(settings.gradle, build.gradle, member-domain/build.gradle, member-api/build.gradle, src의 member-api 이동)를 하나의 커밋으로 묶는 것을 제안
- 0단계(calendar.md 변경, RedisConfigTest 삭제)는 별도 커밋으로 분리해도 되고 함께 묶어도 됨
- docs/db/v1, docs/api/v1 이름 변경은 이미 스테이징되어 있으므로 어느 커밋에 포함할지는 선택

---

# 다음 작업

- 2단계: 패키지 골격, 스캔 설정, 설정 파일 정리
  - domain: dev.choimory.member.domain 아래 common, member, agreement, authority 패키지
  - api: dev.choimory.member.api 아래 config, common, member/v1/command, member/v1/query
  - @EntityScan("dev.choimory.member.domain")과 @EnableJpaRepositories 설정
  - application-datasource.yml의 ES/Redis 설정값과 컨텍스트 테스트의 ES/Redis 속성 제거
  - application-local.yml의 sql init을 always로 변경하고 docs/db의 ddl, dml을 사용하도록 설정, processResources 복사 설정, 옛 sql/schema.sql과 data.sql 삭제

---

# 2단계. 패키지 골격, 스캔 설정, 설정 파일 정리

## 변경 내용

### ES, Redis 설정 제거

- member-api/src/main/resources/application-datasource.yml에서 elasticsearch, data.redis, cache 블록을 제거하고 Postgres(datasource) 설정만 남김
- ChoimoryDevMemberApiApplicationTests의 @SpringBootTest에서 es-*, redis-* 속성을 제거
- ES, Redis 의존성(build.gradle)은 유지. 설정이 없으면 기본값(localhost)으로 자동 구성되며 연결은 사용 시점에 맺어짐

### SQL 초기화 (local 프로필 전용)

- application-local.yml
  - spring.sql.init.mode: always (기존 embedded는 임베디드 DB에서만 동작하여 Postgres에서는 실행되지 않았음)
  - spring.sql.init.schema-locations: classpath:/sql/v1/choimory-dev-member-ddl-v1.sql
  - data-locations는 설정하지 않음 (아래 "계획과 달라진 점" 참고)
  - 앱이 뜰 때마다 실행되므로 DDL은 멱등하게 작성해야 한다는 주석을 남김
- application-dev.yml, application-prod.yml은 변경하지 않음. dev, prod에서는 SQL 초기화가 동작하지 않음
- member-api/build.gradle에 processResources 설정을 추가하여 docs/db 하위(v1/...)를 클래스패스 sql/ 아래로 복사
- 옛 src/main/resources/sql/schema.sql, data.sql과 sql 디렉토리를 삭제

### JpaConfig

- member-api/src/main/java/dev/choimory/member/api/config/JpaConfig.java 신규
  - @EntityScan(basePackages = "dev.choimory.member.domain")
  - @EnableJpaRepositories(basePackages = "dev.choimory.member.api")
- @EnableJpaAuditing은 기존대로 애플리케이션 클래스에 유지
- @EntityScan, @EnableJpaRepositories는 value가 아닌 basePackages로 지정 (리플렉션으로 읽는 테스트에서 별칭이 해석되지 않기 때문)

### 패키지 골격 (package-info.java)

- 빈 패키지는 Git에 남지 않으므로 package-info.java로 패키지의 역할을 기록
- member-domain (dev.choimory.member.domain): common, member, agreement, authority
- member-api (dev.choimory.member.api): common, member/v1/command, member/v1/query
- config 패키지는 JpaConfig가 있으므로 별도 package-info 없음
- 하위 패키지(command/entity, command/dto 등)는 클래스를 추가할 때 함께 생성

## TDD 진행 기록

1. SqlInitProfileTest를 먼저 작성하고 실패를 확인
   - 5건 모두 실패. dev, prod 프로필도 실패한 원인은 application-datasource.yml의 Redis 자리표시자(`${redis-database}`)를 해석하지 못해 컨텍스트가 뜨지 않았기 때문 (ES/Redis 설정 제거의 필요성 확인)
2. ES/Redis 설정 제거, local sql init 설정, processResources 복사, 옛 sql 파일 삭제 후 재실행
   - dev, prod 통과. local 3건 실패: 복사와 설정은 정상이었으나 DDL 파일이 비어 있어 Spring이 `'script' must not be null or empty` 오류를 냄
3. 위 문제에 맞춰 local 테스트를 설정 파일 직접 읽기 방식으로 변경하고 data-locations를 제거한 뒤 통과
4. JpaConfigTest를 먼저 작성하여 JpaConfig 클래스가 없어 컴파일에 실패하는 것을 확인한 뒤 JpaConfig를 구현하고 통과

## 계획과 달라진 점

- Spring은 내용이 없는 SQL 스크립트를 오류로 처리함. 주석만 있는 파일도 마찬가지
  - DML은 이번 범위에서 제외했으므로 data-locations를 설정하지 않음. 초기 데이터가 생겨 내용이 채워질 때 추가해야 함
  - DDL은 비어 있으므로 지금 local 프로필로 앱을 띄우면 실패함. 3단계에서 DDL을 작성하면 해결됨. 그때 local 컨텍스트를 실제로 띄워 DDL을 여러 번 실행해도 통과하는지 확인하는 테스트를 추가할 예정
  - 현재 local 설정 검증은 application-local.yml을 YamlPropertySourceLoader로 직접 읽는 방식 (컨텍스트 로딩 없음)
- 계획의 "빈 패키지 골격" 대신 package-info.java로 골격을 만듦

## 검증 결과

- ./gradlew build 통과 (spotlessApply 적용 후 spotlessCheck 포함)
- 테스트 9건 모두 통과
  - ChoimoryDevMemberApiApplicationTests: 1건 (ES/Redis 속성 없이 컨텍스트 로딩)
  - JpaConfigTest: 2건
  - SqlInitProfileTest: LocalProfile 4건, DevProfile 1건, ProdProfile 1건

## 커밋 단위 제안

- 2단계 전체를 한 커밋으로 묶는 것을 제안
- package-info.java, JpaConfig.java, 테스트 파일은 새 파일이므로 git add가 필요

---

# 다음 작업 (3단계)

- DDL 작성과 id 타입 검증
  1. String id 엔티티 하나로 Postgres uuid 컬럼에 저장, 조회, 네이티브 쿼리, FK, unique, ddl-auto=validate가 되는지 먼저 검증. 안 되면 varchar(36)으로 확정. 검증용 임시 엔티티는 검증 후 삭제
  2. 확정된 타입으로 DB2의 모든 테이블 DDL 작성 (제약조건, COMMENT, 멱등 처리 포함). agreement의 type 컬럼은 제외하고 enum 컬럼은 varchar로 작성
  3. local 컨텍스트가 DDL을 여러 번 실행해도 통과하는지 확인하는 테스트 추가

---

# package-info.java 삭제

- 2단계에서 만든 package-info.java 7개(member-domain 4개, member-api 3개)를 삭제함. 폴더를 유지하려는 용도였으나 클래스를 계속 만들어 갈 것이므로 불필요하다고 판단
- 비어 있던 패키지 디렉토리도 함께 삭제되어 member-domain/src가 없어짐. Gradle은 소스 디렉토리가 없어도 동작하며, 클래스를 추가할 때 디렉토리가 생김
- 삭제 후 ./gradlew build 통과를 확인
- 위 "2단계" 섹션의 package-info.java 관련 기록은 당시 기록이며, 현재는 삭제된 상태

---

# 3단계. DDL 작성과 id 타입 검증

## id 타입 검증 결과

String id를 Postgres uuid 컬럼에 쓸 수 있는지 member-domain 테스트 소스에 임시 엔티티와 테스트를 만들어 검증함. 방식별 결과는 아래와 같고, 검증 코드는 모두 삭제함.

| 방식 | 결과 |
|---|---|
| A. @JdbcTypeCode(SqlTypes.UUID) | 실패. StringJavaType이 String을 UUID로 변환하지 못함 (`Could not convert 'java.lang.String' to 'java.util.UUID'`) |
| D. AttributeConverter<String, UUID> | 실패. JPA 표준상 @Id에는 컨버터가 적용되지 않아 ddl-auto=validate에서 uuid 컬럼을 varchar로 기대함 (`wrong column type encountered in column [id]`) |
| B. @Column(columnDefinition = "uuid") + JDBC URL stringtype=unspecified | 통과(5/5). 다만 Postgres 전용 문자열이 엔티티와 JDBC URL에 들어가므로 채택하지 않음 |

- 결론: "별도 코드는 쓰지 않고, 안 되면 varchar(36)"이라는 결정에 따라 id와 외래키 컬럼을 varchar(36)으로 확정

## DDL 작성 내용

docs/db/v1/choimory-dev-member-ddl-v1.sql

- DB2의 12개 테이블 작성: member, member_image, member_suspension, follow, authority, menu, member_authority, permission, permission_action, agreement, agreement_answer, member_agreement
- 모든 테이블에 공통 컬럼 id, created_at, modified_at, deleted_at 포함 (follow도 deleted_at 유지)
- agreement 테이블의 type 컬럼은 제외
- enum 컬럼(member.status, member_image.type, authority.code, permission_action.action)은 varchar이며 허용 값은 주석으로 기록
- 제약조건
  - member: email, nickname UNIQUE
  - member_agreement: UNIQUE(member_id, agreement_answer_id)
  - member_authority: UNIQUE(member_id, authority_id)
  - follow: UNIQUE(follower_id, followee_id), CHECK(follower_id <> followee_id)
  - authority.code, menu.code UNIQUE
  - permission: UNIQUE(authority_id, menu_id)
  - permission_action: UNIQUE(permission_id, action)
- 외래키 삭제 동작: 소유 관계는 ON DELETE CASCADE, 참조 관계는 ON DELETE RESTRICT
  - CASCADE: member_image, member_suspension, member_authority, member_agreement(member), follow(follower, followee)의 member 참조, permission(authority), permission_action(permission), agreement_answer(agreement)
  - RESTRICT: member_agreement의 agreement_answer 참조(동의 이력 보존), member_authority의 authority 참조, permission의 menu 참조, menu의 부모 메뉴 참조
- FK 컬럼에 인덱스 추가 (unique 제약이 앞쪽 컬럼을 이미 커버하는 경우는 제외)
- 모든 테이블과 컬럼에 COMMENT 작성
- create table/index if not exists로 멱등하게 작성, DROP 없음. 주석에 세미콜론을 쓰지 않음 (Spring의 스크립트 분리가 깨지므로)

## 테스트 인프라와 테스트

- member-domain/build.gradle: 테스트에 시스템 프로퍼티 docs.db.ddl(DDL 파일 경로)을 전달하고, DDL 파일이 바뀌면 테스트가 다시 실행되도록 inputs.file 지정
- member-domain 테스트 인프라 (src/test)
  - TestDomainApplication: 이 모듈에는 애플리케이션 클래스가 없으므로 슬라이스 테스트용 @SpringBootConfiguration, @EnableAutoConfiguration 클래스
  - TestcontainersConfiguration: Postgres 16 컨테이너, docs.db.ddl이 가리키는 DDL을 /docker-entrypoint-initdb.d/01-ddl.sql로 복사하여 초기화
- DdlScriptTest (23건, @JdbcTest, TDD로 먼저 작성하여 테이블이 없어 실패하는 것을 확인한 뒤 DDL 작성)
  - 구조: 전체 테이블 존재, 공통 컬럼, id와 *_id가 varchar(36), enum 컬럼이 varchar, agreement에 type 없음, DDL 멱등성
  - 제약: member email과 nickname 유일(soft delete된 행 포함), member_agreement, member_authority, follow, authority.code, permission, permission_action, menu.code 유일, 자기 팔로우 CHECK
  - 삭제 동작: member 삭제 시 소유 하위 데이터 CASCADE(다른 회원, 권한, 약관, 답변은 유지), authority 삭제 시 permission과 permission_action CASCADE, agreement 삭제 시 답변 CASCADE, 선택된 답변과 부여된 권한과 하위 메뉴가 있는 메뉴와 permission이 참조하는 메뉴는 RESTRICT
- member-api LocalProfileSqlInitContextTest (2건): local 프로필로 앱이 뜰 때 sql init이 DDL을 실행해 12개 테이블이 생성되는지, 클래스패스로 복사된 DDL을 다시 실행해도 오류가 없는지

## 임의로 정한 부분

계획에 없거나 DB2에서 정해지지 않아 판단한 부분임. 바꾸고 싶으면 DDL과 테스트를 수정함.

- 스키마를 지정하지 않음. 옛 schema.sql은 choimory_dev_member 스키마를 사용했으나 DB에 따라 달라지는 코드를 피하려는 원칙에 따름
- NOT NULL 범위: 필수로 보이는 컬럼은 모두 NOT NULL (menu.path, member_image.resize_file_*, member_suspension.suspended_to 포함). NULL 허용으로 푸는 것은 하위 호환 변경이지만 반대는 아니므로 엄격하게 시작
- created_at에 DB 기본값 없음. JPA Auditing이 채움
- 문자열 길이: email, password, nickname 255, name 계열 100~255, path 500, reason 500
- 외래키 인덱스를 추가하고 인덱스와 제약 이름은 idx_, uk_, fk_, ck_, pk_ 접두사 규칙으로 작성

## 검증 결과

- ./gradlew build 통과
- 테스트 34건 모두 통과
  - member-domain: DdlScriptTest 23건
  - member-api: ChoimoryDevMemberApiApplicationTests 1건, JpaConfigTest 2건, LocalProfileSqlInitContextTest 2건, SqlInitProfileTest 6건 (LocalProfile 4, DevProfile 1, ProdProfile 1)

## 커밋 단위 제안

- 3단계 전체(DDL, member-domain 테스트 인프라와 DdlScriptTest, member-api LocalProfileSqlInitContextTest, member-domain/build.gradle)를 하나의 커밋으로 묶는 것을 제안
- 이름 변경이 스테이징된 docs/db/v1의 DDL 파일은 내용이 채워졌으므로 이 커밋에 포함하는 것이 자연스러움

---

# 4단계. BaseEntity

## 구현 내용

member-domain/src/main/java/dev/choimory/member/domain/common/command/entity/BaseEntity.java

- @MappedSuperclass, @EntityListeners(AuditingEntityListener.class), @Getter, Persistable<String> 구현
- 필드: id(varchar 36, 수정 불가), createdAt(수정 불가, not null), modifiedAt, deletedAt, 그리고 @Transient newEntity(새 엔티티 여부)
- id 채번: `protected static String newId()`가 UuidCreator.getTimeOrderedEpoch()로 UUID v7을 생성. 하위 엔티티가 정적 팩토리에서 `super(newId())`로 호출. JPA용 protected 기본 생성자는 채번하지 않음
- Persistable: isNew()가 newEntity를 반환하고, @PostLoad, @PostPersist 콜백이 false로 바꿈. 새로 만든 엔티티는 true
- Auditing: @CreatedDate, @LastModifiedDate
- softDelete(): deletedAt을 LocalDateTime.now()로 설정
- Javadoc에 각 동작과 아래 "계획과 달라진 점"을 기록

## 계획과 달라진 점

1. @SQLRestriction은 @MappedSuperclass에서 하위 엔티티로 상속되지 않음
   - plan3, plan4에서 "구현 때 테스트로 확인"하기로 한 항목
   - 검증: BaseEntity에 어노테이션을 붙였을 때 JPQL, 레포지토리 findAll, findById, em.find 4가지 모두 soft delete된 행을 반환함. 테스트 전용 엔티티 클래스에 어노테이션을 직접 붙이자 4가지 모두 제외됨 (네이티브 쿼리는 어느 경우든 삭제된 행을 조회)
   - 결정: BaseEntity에서 @SQLRestriction을 제거하고 각 엔티티가 `@SQLRestriction("deleted_at IS NULL")`을 직접 선언
   - 빠뜨림 방지: EntityRulesTest가 dev.choimory.member.domain 아래에서 BaseEntity를 상속한 모든 구체 클래스를 찾아 선언 여부를 검사. 6단계에서 엔티티를 추가하면서 빠뜨리면 실패함
2. @EnableJpaAuditing을 TestDomainApplication에 두면 엔티티가 없는 @JdbcTest(DdlScriptTest)가 `JPA metamodel must not be empty`로 실패함
   - 별도 테스트 설정 JpaAuditingTestConfiguration(@TestConfiguration, @EnableJpaAuditing)을 만들고 엔티티를 사용하는 JPA 테스트에서만 @Import

## 테스트

- BaseEntityTest 15건 (@DataJpaTest, Testcontainers Postgres 16)
  - id: UUID v7 형식(36자, 버전 7), 나중에 만든 id가 사전식으로 뒤(시간 순서), JPA 기본 생성자는 id를 채번하지 않음
  - Persistable: 새 엔티티 isNew true, 저장 후 false, 조회 후 false이며 id 유지, 미리 채번한 새 엔티티를 save해도 SELECT 없이 INSERT 한 번만 실행됨(Hibernate 통계로 확인)
  - Auditing: 저장 시 createdAt, modifiedAt이 채워지고 deletedAt은 null, 수정 시 modifiedAt만 갱신되고 createdAt은 유지
  - soft delete: softDelete()가 deletedAt을 채움, soft delete된 행은 JPQL, findAll, findById(em.find 포함)에서 제외, 삭제되지 않은 행은 조회됨, 삭제된 행도 네이티브 쿼리로는 조회됨
- EntityRulesTest 2건: 실제 엔티티 패키지에 필터 선언이 없는 엔티티가 없음(현재는 엔티티가 없어 통과), 검사 로직이 필터 없는 클래스(NoRestrictionEntity)를 실제로 찾아내고 필터가 있는 TestBaseEntity는 통과시킴
- 테스트 전용 클래스는 member-domain/src/test의 dev.choimory.member.testsupport 패키지(TestBaseEntity, TestBaseEntityRepository, NoRestrictionEntity)에 두어 실제 엔티티 스캔과 8단계의 ddl-auto=validate에 영향이 없게 함. BaseEntityTest는 중첩 @TestConfiguration의 @EntityScan, @EnableJpaRepositories로 이 패키지만 스캔하고, 테스트용 테이블(test_base_entity)은 @Sql로 생성

## 남은 선택 사항

- Auditing 기본 동작으로 저장 시 modified_at도 created_at과 같은 값으로 채워짐. DDL의 modified_at은 nullable이므로 "수정된 적 없으면 null"을 원하면 @EnableJpaAuditing(modifyOnCreation = false)로 변경 가능. 현재는 Spring 기본 동작을 유지하고 있고 결정이 필요함

## 검증 결과

- ./gradlew build 통과
- 테스트 51건 모두 통과
  - member-domain 40건: EntityRulesTest 2건, BaseEntityTest 15건, DdlScriptTest 23건
  - member-api 11건

## 커밋 단위 제안

- 4단계 전체(BaseEntity, 테스트 전용 클래스, 테스트 설정, 테스트)를 하나의 커밋으로 묶는 것을 제안

---

# 5단계. enum

## 구현 내용

member-domain/src/main/java/dev/choimory/member/domain 아래에 enum 4개를 추가함. 각 값에 설명 주석을 달고 클래스 Javadoc에 "DB에 이름(문자열)으로 저장되므로 이름을 변경하거나 삭제하면 기존 데이터가 깨진다"를 기록함.

| enum | 값 | 패키지 | 대응 컬럼 |
|---|---|---|---|
| MemberStatus | REGISTER, ACTIVATE, INACTIVATE, SUSPENDED | member/command/type | member.status |
| MemberImageType | PROFILE | member/command/type | member_image.type |
| AuthorityCode | MEMBER, ADMIN, MODERATOR | authority/command/type | authority.code |
| PermissionActionType | CREATE, READ, UPDATE, DELETE | authority/command/type | permission_action.action |

- enum은 DB 네이티브 enum이 아니라 서버에서만 처리하며, 엔티티에서는 @Enumerated(EnumType.STRING)으로 varchar 컬럼에 매핑할 예정 (6단계에서 매핑 검증)
- 패키지는 엔티티, DTO와 같은 command 아래의 type 패키지

## 계획과 달라진 점

- 계획의 enum 이름 `PermissionAction`을 `PermissionActionType`으로 변경. 엔티티 이름 규칙(엔티티는 테이블명 그대로)에 따라 permission_action 테이블의 엔티티가 `PermissionAction`이 되므로 이름이 겹침
- 계획의 AgreementType은 agreement.type 컬럼을 제외하기로 하여 만들지 않음

## 테스트

- EnumValuesTest 4건: 각 enum의 값과 순서가 DB2, DDL 주석의 허용 값과 같은지 확인. 값의 이름이 의도치 않게 바뀌거나 삭제되는 것을 막기 위한 테스트
- TDD: 테스트를 먼저 작성하여 enum 패키지가 없어 컴파일에 실패하는 것을 확인한 뒤 enum을 구현
- @Enumerated(EnumType.STRING) 매핑 검증은 엔티티가 있는 6단계에서 함께 진행

## 남은 선택 사항

- 4단계에서 남은 modifyOnCreation 결정이 아직 남아 있음. 저장 시 modified_at이 created_at과 같은 값으로 채워지는 현재 동작을 유지할지, "수정된 적 없으면 null"로 바꿀지(@EnableJpaAuditing(modifyOnCreation = false)) 6단계 엔티티 작성 전에 결정 필요

## 검증 결과

- ./gradlew build 통과 (spotlessApply 적용 후)
- 테스트 55건 모두 통과
  - member-domain 44건: EnumValuesTest 4건, EntityRulesTest 2건, BaseEntityTest 15건, DdlScriptTest 23건
  - member-api 11건

## 커밋 단위 제안

- 5단계(enum 4개, EnumValuesTest)는 4단계와 같은 커밋으로 묶거나 별도 커밋으로 분리해도 됨

---

# 6단계. 엔티티

## 6단계 전에 결정된 사항

1. local 프로필의 sql init은 `mode: always`를 유지함 (embedded는 임베디드 DB에서만 동작하므로 Postgres에서는 스크립트가 실행되지 않음)
2. modified_at은 수정 이력이 없으면 null
   - `@EnableJpaAuditing(modifyOnCreate = false)`를 ChoimoryDevMemberApiApplication과 member-domain 테스트 설정(JpaAuditingTestConfiguration)에 적용
   - 4단계와 5단계 섹션에서 이 속성을 `modifyOnCreation`이라고 적은 것은 오기이며 실제 이름은 `modifyOnCreate`임 (컴파일 오류로 확인)
   - 테스트 변경: BaseEntityTest의 저장 시 modifiedAt이 null, 처음 수정할 때 채워지고 이후 수정마다 갱신되는지 확인(2건 수정), JpaConfigTest에 앱의 Auditing 설정 확인 1건 추가 (먼저 테스트를 바꿔 3건이 실패하는 것을 확인한 뒤 설정 변경)

## 구현한 엔티티

member-domain의 각 패키지 command/entity에 12개. 모두 BaseEntity를 상속하고 `@SQLRestriction("deleted_at IS NULL")`을 선언하며 Lombok `@Getter`, `@NoArgsConstructor(access = PROTECTED)`를 사용. 테이블명과 컬럼명은 `@Table`, `@Column`으로 명시하고 enum은 `@Enumerated(EnumType.STRING)`.

| 패키지 | 엔티티 | 하위 컬렉션 (cascade) |
|---|---|---|
| member | Member | images, suspensions, authorities, agreements (ALL) |
| member | MemberImage, MemberSuspension | 없음 |
| member | Follow | 없음. Member에 컬렉션을 두지 않고 Follow의 @ManyToOne으로만 연결 |
| authority | Authority | permissions (ALL) |
| authority | Permission | actions (ALL), 메뉴는 참조만 |
| authority | PermissionAction, MemberAuthority | 없음 |
| authority | Menu | children (PERSIST, MERGE만) |
| agreement | Agreement | answers (ALL) |
| agreement | AgreementAnswer, MemberAgreement | 없음 |

- 연관관계는 모두 @ManyToOne(fetch = LAZY, optional = false), 단 Menu의 parent는 nullable
- orphanRemoval은 어디에도 사용하지 않음. 참조만 하는 관계(MemberAuthority→Authority, MemberAgreement→AgreementAnswer, Permission→Menu, Follow→Member)에는 cascade 없음
- 자식은 정적 팩토리 create()에서 부모 컬렉션(addXxx)에 스스로 등록. 부모만 저장해도 cascade로 자식이 함께 저장됨
- Follow.create()는 자기 팔로우를 IllegalArgumentException으로 막음 (DB의 CHECK 제약과 같은 규칙)
- Member의 수정 메소드: updateProfile, changePassword, changeStatus (계획에 명시되지 않았으나 컬럼에 대응하는 최소한의 수정 메소드)
- id는 엔티티가 BaseEntity의 `newId()`를 생성자에서 호출하여 채번

## Builder 적용

- 컨벤션(code-convention-springboot.md)은 DTO, 요청 객체, 응답 객체의 정적 팩토리를 Builder 패턴으로 작성하도록 하고, Entity 예시는 private 생성자를 new로 호출하는 방식임
- 결정: 엔티티의 create()도 생성자를 직접 호출하지 않고 Builder로 생성해 반환
  - private 생성자에 `@Builder(access = AccessLevel.PRIVATE)`를 붙이고 create()는 `X.builder()...build()`로 생성. id 채번(super(newId()))은 생성자에 있으므로 유지되고, 부모 컬렉션 등록은 create()에 그대로 있음
  - 12개 엔티티 모두 적용. src/main에서 엔티티를 new로 직접 생성하는 곳 없음
- EntityRulesTest 규칙 추가
  - 실제 엔티티가 모두 Lombok Builder 클래스({클래스명}Builder)를 가졌는지 확인
  - 검사 로직이 Builder 없는 클래스(테스트 전용 엔티티)를 실제로 잡아내는지 확인
  - 먼저 규칙 테스트를 추가하여 12개가 위반으로 걸리는 것을 확인한 뒤 리팩터링
- 컨벤션 문서의 Entity 예시와 달라지는 부분이므로 문서를 같은 방식으로 맞출지는 미정 (문서는 수정하지 않음)

## 테스트

TDD: 테스트를 먼저 작성하여 엔티티 클래스가 없어 컴파일에 실패하는 것을 확인한 뒤 구현. 엔티티 테스트는 `@JpaEntityTest`(@DataJpaTest, Testcontainers Postgres 16에 docs/db의 DDL 적용, `ddl-auto=validate`)로 실행하므로 엔티티 매핑이 DDL과 어긋나면 컨텍스트가 뜨지 않음. 8단계의 DDL 정합성 검증을 상당 부분 앞당겨 확인한 셈.

- 테스트 도구: JpaEntityTest(메타 어노테이션), EntityFixtures(unique 제약에 걸리지 않는 객체 생성)
- MemberEntitiesTest 13건: Member 생성, 수정 메소드, 저장과 조회, enum 문자열 저장, email 중복 거부, soft delete된 회원 제외(네이티브로는 조회), 회원 삭제 시 하위 데이터 cascade 삭제, 이미지와 정지 이력의 등록 및 cascade 저장과 조회, soft delete된 이미지는 컬렉션에서 제외, Follow의 자기 팔로우 거부, 저장과 조회, hard delete, 중복 거부
- AuthorityEntitiesTest 5건: 권한 저장과 코드 문자열 저장, 하위 메뉴 등록과 cascade, 하위 메뉴가 있는 메뉴 삭제 실패, permission과 action의 cascade 저장과 조회와 action 문자열 저장, 회원 권한 cascade와 회원 삭제 시 권한은 남음
- AgreementEntitiesTest 5건: 약관 생성, 선택 항목 등록과 cascade, soft delete된 선택 항목은 컬렉션에서 제외, 회원 동의 이력 cascade와 회원 삭제 시 선택 항목은 남음, 중복 동의 거부
- EntityRulesTest: 이제 실제 엔티티 12개를 검사하여 통과(더 이상 빈 검사가 아님)

## 임의로 정한 부분

- Member.create()는 회원 상태를 인자로 받음. 가입 시 기본 상태(REGISTER)로 고정할지는 서비스 구현 때 정해도 됨
- 엔티티 테스트용 헬퍼로 JpaEntityTest, EntityFixtures를 만듦

## 검증 결과

- ./gradlew build 통과
- 테스트 81건 모두 통과
  - member-domain 69건: EntityRulesTest 4건, EnumValuesTest 4건, AgreementEntitiesTest 5건, AuthorityEntitiesTest 5건, BaseEntityTest 15건, DdlScriptTest 23건, MemberEntitiesTest 13건
  - member-api 12건

## 커밋 단위 제안

- modified_at 설정 변경(modifyOnCreate), 6단계 엔티티, Builder 적용을 하나의 커밋으로 묶거나 modifyOnCreate 변경만 별도 커밋으로 분리해도 됨

---

# 6단계 후속 변경. 엔티티 생성 방식 정리

6단계 구현 후 엔티티 생성 방식을 아래와 같이 바꿈. 위 "6단계"의 "Builder 적용" 섹션은 바뀌기 전 기록이며 이 섹션이 최종임.

## 요청 사항

1. Builder 어노테이션을 클래스 레벨에 부여
2. 엔티티에 Lombok AllArgsConstructor 추가
3. @Table 제거 (테이블 이름은 엔티티 이름을 따르므로 불필요)

## 논의와 결정

- 생성자와 Builder 접근 수준
  - @NoArgsConstructor(PROTECTED): JPA가 DB에서 읽을 때 기본 생성자가 필요하고 private이면 안 되므로 필요한 최소 공개 범위. 컨벤션의 Entity 예시와 같음
  - private 생성자와 private Builder: 컨벤션에 없는 선택으로, 생성 경로를 create() 하나로 막으려는 의도. create()가 부모 컬렉션 등록(부모 저장 시 cascade로 함께 저장되도록)과 생성 규칙(자기 팔로우 검사)을 담당하므로 우회되면 자식 누락 같은 문제가 조용히 생김
- 외부(예: member-api의 Request.toEntity())에서 Builder를 쓸 수 있는지
  - 클래스 레벨 private Builder만 두면 외부에서는 create() 위치 인자만 가능(인자가 많은 MemberImage는 불편)
  - 클래스 레벨 public Builder는 build()가 생성자를 직접 호출하므로 create()의 규칙을 우회할 수 있음
  - 결정(B안): create() 메소드에 @Builder를 붙여 외부용 Builder가 항상 create()를 거치도록 함

## 최종 구조

각 엔티티(12개)는 아래 구조.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE, builderMethodName = "initBuilder", builderClassName = "MemberInit")
public class Member extends BaseEntity {
    // 컬렉션 필드는 @Builder.Default
    // ...

    @Builder // 외부용 Builder(Member.builder()). build()가 create()를 호출한다
    public static Member create(...) {
        return Member.initBuilder()...build(); // 인스턴스는 private Builder로 생성한다. 생성자를 직접 호출하지 않는다
    }
}
```

- 외부용 Builder: create()에 붙인 @Builder (`X.builder()`, 클래스 `XBuilder`, public). build()가 create()를 호출하므로 부모 등록과 생성 규칙을 우회할 수 없음
- 인스턴스 생성용 Builder: 클래스 레벨 @Builder(access = PRIVATE). 이름이 겹치지 않도록 `initBuilder()`, `XInit`으로 지정. create() 안에서만 사용
- 컬렉션 필드(images, suspensions, authorities, agreements, permissions, actions, children, answers)에 @Builder.Default. 클래스 레벨 @Builder는 필드 초기값(new ArrayList<>())을 무시하기 때문
- @Table 제거. 테이블 이름은 엔티티 이름을 snake_case로 변환한 이름 (Member → member, MemberImage → member_image, PermissionAction → permission_action). ddl-auto=validate 테스트가 통과하므로 DDL과 일치함

## id 채번 위치 변경

- Lombok이 만드는 생성자는 부모의 기본 생성자(super())를 호출하므로 손으로 쓴 생성자의 `super(newId())`를 쓸 수 없게 됨
- BaseEntity의 기본 생성자에서 UUID v7을 채번하도록 변경. BaseEntity(String id) 생성자를 제거하고 newId()는 private으로 변경
- 효과: 어떤 생성자나 Builder로 만들어도 채번을 빠뜨릴 수 없음
- 대가: JPA가 DB에서 읽을 때도 기본 생성자가 호출되어 UUID를 한 번 만들고 곧바로 DB 값으로 덮어씀 (비용은 무시할 수준). 이전에 정한 "JPA 기본 생성자는 채번하지 않음"을 번복한 것
- 테스트 변경: BaseEntityTest의 "JPA 기본 생성자는 id를 채번하지 않는다"를 "JPA 기본 생성자도 id를 채번한다"로 변경하고, "DB에서 읽은 엔티티는 저장된 id를 유지한다" 추가. 테스트 전용 엔티티(TestBaseEntity)도 같은 방식으로 변경

## 테스트

TDD: 테스트를 먼저 바꿔 빨간 상태를 확인한 뒤 구현.

- EntityRulesTest: @Table을 붙이지 않는 규칙(실제 엔티티 검사, 검사 로직 검증용 테스트 전용 클래스 TableAnnotatedEntity) 추가. 기존 Builder 규칙은 create()의 @Builder가 만드는 `{클래스명}Builder` 클래스로도 통과
- MemberEntitiesTest: 외부 Builder도 create()를 거쳐 회원의 이미지 목록에 등록되는지, 외부 Builder로도 자기 팔로우 검사를 우회할 수 없는지 2건 추가

## 진행 중 발생한 문제

1. 사용자가 중단한 명령이 일부 적용되어 있었음 (BaseEntityTest, EntityRulesTest, TableAnnotatedEntity). 이후 같은 수정을 다시 적용하려다 첫 단계에서 실패. 파일 상태를 확인하고 남은 부분(MemberEntitiesTest의 Builder 테스트)만 적용
2. 변환 스크립트가 포매터가 여러 줄로 나눈 @OneToMany(Menu.children)를 매칭하지 못해 @Builder.Default가 빠짐. 빌드의 Lombok 경고("@Builder will ignore the initializing expression")와 테스트 2건 실패로 발견하여 수정. 이후 src/main의 모든 List 필드에 @Builder.Default가 있는지 확인

## 검증 결과

- ./gradlew build 통과, Lombok 경고 없음
- 테스트 86건 모두 통과 (member-domain 74건, member-api 12건)

## 컨벤션 문서와의 차이

- 컨벤션의 Entity 예시는 private 생성자를 new로 호출하는 방식임. 이번 결정(create()가 Builder로 생성하고 외부용 Builder도 create()를 거침)과 다르므로 문서를 맞출지는 미정 (문서는 수정하지 않음)

---

# 7단계. 엔티티 DTO

## 결정 사항

- 상위 id를 일반 필드로도 두지 않음. 컨벤션 그대로 "하위 DTO만 포함하고 상위는 전혀 포함하지 않는다"로 하고, 상위가 필요하면 조회 쪽에서 해결함 (약관에서 선택 항목을 펼쳐 보거나, 선택 항목을 조회해 얻은 id로 상위 약관을 다시 조회). 따라서 MemberAgreementDto는 어느 약관인지 담지 않음
- 참조만 하는 관계는 기본안으로 진행: 참조 대상의 DTO를 자신의 하위 컬렉션을 null로 둔 상태로 담음. 사용해 보고 별로면 수정하기로 함

## 구현 내용

member-domain의 각 패키지 command/dto에 엔티티와 1:1인 DTO 12개. 형식은 `record` + `@Builder(toBuilder = true)`이고 변환은 정적 팩토리 `from(Entity)`가 `builder()`로 조립. 컬렉션은 stream으로 변환.

| 패키지 | DTO | 하위 DTO 컬렉션 | 참조 대상 (하위 컬렉션 null) |
|---|---|---|---|
| member | MemberDto | images, suspensions, authorities, agreements | |
| member | MemberImageDto, MemberSuspensionDto | 없음 | |
| member | FollowDto | 없음 | follower, followee (MemberDto) |
| authority | AuthorityDto | permissions | |
| authority | PermissionDto | actions | menu (MenuDto) |
| authority | PermissionActionDto | 없음 | |
| authority | MenuDto | children (부모는 포함하지 않음) | |
| authority | MemberAuthorityDto | 없음 | authority (AuthorityDto) |
| agreement | AgreementDto | answers | |
| agreement | AgreementAnswerDto | 없음 | |
| agreement | MemberAgreementDto | 없음 | agreementAnswer (AgreementAnswerDto) |

- 모든 DTO에 id와 공통 일시(createdAt, modifiedAt, deletedAt)를 포함
- 하위 DTO에는 상위 필드가 없음 (MemberImageDto에 member, memberId 없음 등)
- `fromWithoutChildren(Entity)`: MemberDto, AuthorityDto, MenuDto에 추가. 엔티티의 지연 로딩 컬렉션을 읽지 않고 하위 컬렉션을 null로 둠. 참조 관계에서 사용하며 `from()`이 이를 재사용(`fromWithoutChildren(entity).toBuilder().children(...)`)
- from()은 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 함. 이 내용을 Javadoc에 기록

## 테스트

TDD: 테스트를 먼저 작성하여 DTO 클래스가 없어 컴파일에 실패하는 것을 확인한 뒤 구현.

- MemberDtosTest 7건: 필드와 소유한 하위 DTO 매핑(DB 미사용), 공통 일시 복사, 하위 DTO에 상위 필드 없음, 지연 로딩 컬렉션을 트랜잭션 안에서 변환하면 하위 DTO까지 채워짐, fromWithoutChildren은 컬렉션이 null이고 엔티티의 컬렉션을 로딩하지 않음(Hibernate.isInitialized로 확인), FollowDto의 follower와 followee는 하위 컬렉션이 null이고 로딩되지 않음, toBuilder 복사
- AuthorityDtosTest 5건: 권한 DTO의 permission과 action 매핑과 메뉴는 하위 메뉴가 null, 메뉴 DTO는 하위 메뉴만 포함하고 부모는 필드에 없음, 메뉴 fromWithoutChildren, 하위 DTO에 상위 필드 없음, 회원 권한 DTO의 권한은 permissions가 null이고 로딩되지 않음(회원 DTO 변환 시)
- AgreementDtosTest 4건: 약관과 선택 항목 매핑, 선택 항목과 동의 이력 DTO에 상위 필드 없음, 동의 이력 DTO의 선택 항목 매핑, soft delete된 선택 항목은 DTO에서도 제외
- DtoRulesTest 4건 (규칙 테스트): 모든 DTO가 record이고 이름이 Dto로 끝남, 엔티티와 DTO가 정확히 1:1(이름 집합 비교), 모든 DTO가 `from(Entity)`와 `toBuilder()`와 Builder 클래스를 가짐, 엔티티에 DTO 변환 메소드가 없음. 새 엔티티에 DTO를 빠뜨리면 실패

## 진행 중 발생한 문제

- DtoRulesTest의 스캔 필터(`.*\.command\.dto\..*`)가 같은 패키지에 있는 테스트 클래스(MemberDtosTest 등)까지 DTO로 잡아 3건이 실패. 필터를 이름이 Dto로 끝나는 클래스(`.*\.command\.dto\.\w+Dto`)로 좁혀 해결

## 검증 결과

- ./gradlew build 통과
- 테스트 106건 모두 통과 (member-domain 94건, member-api 12건)

## 커밋 단위 제안

- 6단계 후속 변경(엔티티 생성 방식)과 7단계(DTO)를 각각 별도 커밋으로 나누는 것을 제안. 파일 수가 많음

---

# 8단계. DDL과 엔티티 정합성 검증

## 이미 확인되고 있던 것

- ddl-auto=validate: 모든 엔티티 테스트(@JpaEntityTest)가 docs/db의 DDL이 적용된 Postgres에서 validate로 실행되므로 엔티티 매핑이 DDL과 어긋나면 컨텍스트가 뜨지 않음 (6단계)
- unique, 자기 팔로우 CHECK, 외래키 CASCADE와 RESTRICT: DdlScriptTest(SQL 수준)와 엔티티 테스트(엔티티 수준)에서 확인
- 삭제된 행을 네이티브 쿼리로 조회: BaseEntityTest와 MemberEntitiesTest에서 확인

## 추가한 검증

ddl-auto=validate는 컬럼 타입만 비교하므로 아래를 잡지 못함: NOT NULL 여부, DDL에만 있는 컬럼, 엔티티가 매핑하지 않은 컬럼. 이를 보완하는 EntityMappingTest(3건)를 추가. Hibernate가 실제로 매핑한 정보(SessionFactoryImplementor의 매핑 메타모델)와 DDL(information_schema)을 비교.

- entityTablesMatchDdlTables: 엔티티가 매핑한 테이블 목록이 DDL의 테이블 목록(12개)과 같음
- entityColumnsMatchDdlColumns: 테이블마다 엔티티의 컬럼 목록이 DDL의 컬럼 목록과 같음
- entityNullabilityMatchesDdl: 컬럼마다 엔티티의 NULL 허용 여부가 DDL과 같음

결과: 세 가지 모두 일치. 실제 엔티티와 DDL 사이에 불일치는 발견되지 않음.

## 진행 중 발생한 문제

- NULL 허용 여부 테스트가 처음에 `agreement.id`가 NULL 허용이라고 실패. 실제 불일치가 아니라 테스트의 오류였음: @OneToMany 컬렉션 속성의 "컬럼"을 조회하면 Hibernate가 소유자의 id 컬럼을 돌려주어 식별자 컬럼의 NOT NULL을 덮어씀. 컬렉션 속성(`isCollectionType()`)을 건너뛰도록 수정
- 검사가 빈 검사가 아닌지 확인: 일부러 Member.nickname의 `nullable`을 틀리게(true) 바꿔서 실행하자 `member.nickname 의 NULL 허용 여부`로 정확히 실패하는 것을 확인한 뒤 원래 코드로 되돌림 (되돌린 뒤 빌드 통과 확인)
- 테스트 컴파일 시 Hibernate의 deprecated API(AbstractEntityPersister) 사용 안내가 나옴. 테스트 코드이고 경고만 나오므로 그대로 둠

## 검증 결과

- EntityMappingTest 3건 통과
- 계획의 8단계 항목(validate, unique, 자기 팔로우 CHECK, CASCADE, menu 부모 FK RESTRICT, 삭제된 행의 네이티브 조회) 모두 테스트로 확인됨

---

# 9단계. 최종 점검

## 점검 내용

- `./gradlew spotlessApply` 적용 후 `./gradlew clean build` 통과 (spotlessCheck 포함)
- 테스트 109건 모두 통과, 건너뛴 테스트 없음, 경고 없음
  - member-domain 97건: DdlScriptTest 23건, BaseEntityTest 16건, MemberEntitiesTest 15건, AuthorityEntitiesTest 5건, AgreementEntitiesTest 5건, EnumValuesTest 4건, EntityRulesTest 6건, DtoRulesTest 4건, EntityMappingTest 3건, MemberDtosTest 7건, AuthorityDtosTest 5건, AgreementDtosTest 4건
  - member-api 12건: ChoimoryDevMemberApiApplicationTests 1건, JpaConfigTest 3건, LocalProfileSqlInitContextTest 2건, SqlInitProfileTest 6건

## 계획 대비 완료 상태

| 단계 | 내용 | 상태 |
|---|---|---|
| 0 | 시작 준비 (calendar TASK, RedisConfigTest 삭제) | 완료 |
| 1 | Gradle 멀티 모듈 전환 | 완료 |
| 2 | 패키지 골격, 스캔 설정, 설정 파일 정리 | 완료 (package-info.java는 이후 삭제) |
| 3 | DDL 작성과 id 타입 검증 (varchar(36) 확정) | 완료 |
| 4 | BaseEntity | 완료 (@SQLRestriction 상속 안 됨 확인 후 엔티티마다 선언) |
| 5 | enum | 완료 (PermissionAction enum은 PermissionActionType으로 변경) |
| 6 | 엔티티 12개 (+ 후속: 생성 방식 정리) | 완료 |
| 7 | 엔티티 DTO 12개 | 완료 |
| 8 | DDL과 엔티티 정합성 검증 | 완료 |
| 9 | 최종 점검 | 완료 |

## 남은 것

- "작업 마무리" 지시 시 진행할 것: done1.md 작성, calendar.md를 TASK에서 DONE으로 변경, README.md 갱신 (README는 삭제 전 구조를 설명하고 있어 전면 갱신 필요)
- 컨벤션 문서(code-convention-springboot.md)의 Entity 예시를 이번에 정한 생성 방식(create()가 Builder로 생성하고 외부용 Builder도 create()를 거침)에 맞출지 미정 (문서는 수정하지 않음)
- DDL의 COMMENT를 각 create table 바로 아래로 옮길지 미정 (Postgres는 컬럼 옆에 인라인 COMMENT를 쓸 수 없고 별도 COMMENT ON 문이 필요하며, 지금은 파일 맨 아래에 모아 둔 상태)
- plan1.md~plan4.md의 기존 본문에는 결정이 바뀐 부분(uuid 타입, QueryDSL, AgreementType, member-model 등)이 남아 있으며 규칙상 수정하지 않았고, 각 문서 후반부에 변경 경위가 기록되어 있음

## 커밋 단위 제안

1. 0~1단계: 멀티 모듈 전환, RedisConfigTest 삭제, calendar 변경
2. 2~3단계: 설정 파일 정리, DDL
3. 4~5단계: BaseEntity, enum, modifyOnCreate
4. 6단계: 엔티티와 후속 변경(생성 방식)
5. 7~8단계: DTO, 정합성 테스트

---

# 엔티티 create() 제거와 클래스 레벨 Builder 전환

> 그냥 엔티티 create 없애고 builder도 클래스에만 놓고 tobuilder true로 하자 컨벤션대로

> 진행해줘 add 편의메소드는 유지

> tobuilder 빼고 ㄱㄱ

위 "6단계 후속 변경"의 B안(create()에 @Builder)을 대체함. 엔티티 생성은 컨벤션대로 클래스 레벨 Builder만 사용함. toBuilder는 논의 후 쓰지 않기로 함. (엔티티의 toBuilder()는 id와 공통 일시를 복사하지 않아 새 엔티티가 만들어지므로 수정 용도로 쓸 수 없음)

## 변경 내용

- 엔티티 11개(BaseEntity 제외 구체 엔티티 전부)에서 create() 정적 팩토리와 private Builder(initBuilder, XInit) 제거
- `@Builder`를 클래스 레벨에 기본 접근 수준(public)으로 부여, `@AllArgsConstructor(PRIVATE)`와 `@NoArgsConstructor(PROTECTED)`는 유지, 컬렉션 필드의 `@Builder.Default`도 유지
- 부모의 add 편의 메소드(addImage, addSuspension, addAuthority, addAgreement, addPermission, addAction, addAnswer, addChild)는 유지하되 등록한 자식을 반환하도록 변경
  - 사용 예: `member.addImage(MemberImage.builder().member(member)...build())`
- 사라진 동작
  - 부모 컬렉션 자동 등록: Builder만으로는 등록되지 않으며 add 메소드를 직접 호출해야 함
  - 자기 팔로우 검사(Follow.create): 엔티티에서 제거. Service에서 검증하도록 두고 DB의 CHECK가 마지막 방어선

## 테스트 변경

- EntityRulesTest: "정적 create() 없음" 규칙과 검출 로직 확인 테스트 추가 (6건에서 8건). 클래스 레벨 Builder 규칙(`{Name}Builder` 존재)은 그대로
- 테스트와 EntityFixtures의 `X.create(...)` 호출을 Builder 호출로 변환 (부모가 있는 자식은 `parent.addX(...)`로 감쌈)
- MemberEntitiesTest (15건에서 14건)
  - builderGoesThroughCreateAndRegistersToParent를 builderDoesNotRegisterToParentUntilAdded로 변경 (Builder만으로는 등록되지 않고 addImage()를 호출하면 등록됨)
  - followRequiresDifferentMembers와 builderCannotBypassSelfFollowRule을 selfFollowIsRejectedByDatabaseCheck 하나로 통합 (DB CHECK가 거부하는지 확인)

## 검증 결과

- `./gradlew spotlessApply` 후 `./gradlew clean build` 통과, 테스트 110건 모두 통과 (member-domain 98건, member-api 12건)
- 이 변경으로 위 "9단계. 최종 점검"의 테스트 수(109건)와 "남은 것"의 컨벤션 Entity 예시 항목(create()가 Builder로 생성하는 방식 기준)은 달라짐. 컨벤션 문서는 수정하지 않았고, 엔티티 생성 방식이 컨벤션과 같아졌으므로 예시와의 차이는 거의 없음
