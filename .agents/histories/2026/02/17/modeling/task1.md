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
