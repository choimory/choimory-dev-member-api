# 목차

- [개요](#개요)
- [구현 계획](#구현-계획)
  - [범위](#범위)
  - [현재 상태](#현재-상태)
  - [진행 규칙](#진행-규칙)
  - [작업 순서](#작업-순서)
  - [테스트 계획](#테스트-계획)
  - [구현 전 확인이 필요한 항목](#구현-전-확인이-필요한-항목)
- [추가1](#추가1)
- [추가1에 대한 의견](#추가1에-대한-의견)
- [추가2](#추가2)
- [추가2에 대한 의견](#추가2에-대한-의견)
- [추가3](#추가3)
- [추가3에 대한 의견](#추가3에-대한-의견)
- [추가4](#추가4)
- [추가4에 대한 의견](#추가4에-대한-의견)
- [최종 구현 계획](#최종-구현-계획)
  - [확정된 결정 사항](#확정된-결정-사항)
  - [cascade 정책](#cascade-정책)
  - [단계별 작업](#단계별-작업)
  - [남은 검증 항목](#남은-검증-항목)

---

# 개요

- plan1에서 회원 DB 설계를 마무리함 (plan1.md의 DB2 섹션 참고)
- plan2에서 모듈명과 패키지 구조를 확정함 (plan2.md의 "최종 모듈명과 패키지 구조" 섹션 참고)
- plan3에서 엔티티 구현 방식을 확정함 (plan3.md 참고)
- 이번 plan4에서는 위 결정들을 코드로 옮기는 구현 계획을 정리함
- 이 문서는 계획만 담고 있으며, 구현은 작업 시작을 지시한 뒤에 단계별로 승인받고 진행함

---

# 구현 계획

## 범위

### 포함

- Gradle 멀티 모듈 전환 (member-domain, member-api)
- member-domain 모듈: BaseEntity, enum, 엔티티 전체, 엔티티 DTO
- DB2 기준 DDL SQL 작성 (docs/db/v1)
- 엔티티와 DDL의 정합성을 검증하는 테스트 인프라 (Testcontainers Postgres)
- member-api 모듈: 애플리케이션 클래스, 스캔 설정, 패키지 골격

### 제외 (이후 작업)

- 컨트롤러, 서비스, 핸들러, 레포지토리, 요청/응답 객체 (API 구현 시 함께 작성)
- 인증, 로그인, Spring Security 설정
- Redis, Elasticsearch 설정과 ES Document, Document DTO (ES 조회 API 만들 때)
- Kafka 연동 (TODO 주석으로만 흐름 표시)
- Flyway 도입
- 스프링 배치 모듈 (member-batch)
- OpenAPI 문서

## 현재 상태

- 단일 모듈이며 src/main에는 ChoimoryDevMemberApiApplication.java 하나만 남아 있음 (기존 소스는 f53eee6 커밋에서 제거됨)
- build.gradle: Java 21, Spring Boot 3.5.4-SNAPSHOT, Spotless + Palantir Java Format, JPA, Security, Validation, Web, Elasticsearch, Redis, Postgres 드라이버, uuid-creator, Lombok, Testcontainers
- docs/db/v1/choimory-dev-member-ddl-v1.sql, choimory-dev-member-dml-v1.sql은 비어 있음 (이전 docs/db/1.0.0에서 이름 변경됨)
- src/main/resources/sql/schema.sql은 옛 member 테이블 정의이고 data.sql은 비어 있음. local 프로필이 sql-init 그룹으로 이 파일을 사용하도록 설정되어 있음
- application.yml은 open-in-view false, ddl-auto none, generate-ddl false
- src/test의 RedisConfigTest는 삭제된 클래스(RedisConfig, MemberEntityDto)를 참조하여 현재 컴파일되지 않음
- README.md는 삭제 전 구조를 설명하고 있어 작업 마무리 시 갱신 필요
- 기존 코드 구조는 git log(f53eee6 이전)로 참고 가능

## 진행 규칙

- git commit과 push는 직접 진행하므로 하지 않음. 각 단계가 끝나면 커밋하기 좋은 단위로 변경 내용을 설명함
- 각 단계 시작 전에 무엇을 할지 설명하고 승인을 받음
- TDD로 진행함. 함수 단위 테스트를 먼저 작성하고 구현함
- 주석 규칙(.agents/rules/back-end/comment.md)과 코드 컨벤션(code-convention-springboot.md)을 따름
- DTO는 README의 기존 규칙대로 record + @Builder(toBuilder = true)를 사용함 (불변)
- 코드 작성 후 spotlessApply로 포맷을 맞춤
- 작업 시작 시 calendar.md를 TASK로 변경하고, 마무리 시 done1.md 작성 후 DONE으로 변경하며 README.md를 갱신함

## 작업 순서

### 0단계. 시작 준비

- calendar.md의 modeling을 TASK로 변경
- 현재 빌드 상태 확인 (컴파일 불가 상태인 RedisConfigTest 처리 방침 결정 필요)
- 산출물: 정리된 빌드 기준선

### 1단계. Gradle 멀티 모듈 전환

- settings.gradle에 member-domain, member-api 포함
- 루트 build.gradle: Spotless, Palantir 설정을 subprojects에 공통 적용
- member-domain: java-library, io.spring.dependency-management로 Spring Boot BOM만 사용 (bootJar 없음). 의존성은 spring-boot-starter-data-jpa(api 스코프), uuid-creator, Lombok. 테스트 의존성으로 Testcontainers Postgres
- member-api: Spring Boot 플러그인, project(':member-domain') 의존, 기존 starter 의존성 이동
- 기존 src를 member-api로 이동
- 검증: ./gradlew build 통과, member-api가 member-domain 클래스를 참조할 수 있음

### 2단계. 패키지 골격과 스캔 설정

- domain 기본 패키지: dev.choimory.member.domain
  - common/command/entity
  - member/command/entity, member/command/dto
  - agreement/command/entity, agreement/command/dto
  - authority/command/entity, authority/command/dto
- api 기본 패키지: dev.choimory.member.api
  - config, common, member/v1/command, member/v1/query
- @EntityScan("dev.choimory.member.domain")과 @EnableJpaRepositories 설정
- 검증: 애플리케이션 컨텍스트 로딩 테스트 통과

### 3단계. DDL SQL 작성 (docs/db/v1/choimory-dev-member-ddl-v1.sql)

- DB2의 모든 테이블 작성. 공통 컬럼 id, created_at, modified_at, deleted_at
- enum 컬럼은 varchar(CHECK 제약 없음). 허용 값은 주석으로 기록
- id는 varchar, 비즈니스 로직에서 UUID v7 채번
- 제약조건
  - member: email, nickname UNIQUE
  - member_agreement: UNIQUE(member_id, agreement_answer_id)
  - follow: UNIQUE(follower_id, followee_id), CHECK(follower_id <> followee_id)
  - permission_action: UNIQUE(permission_id, action)
  - 하위 테이블 FK는 ON DELETE CASCADE (member hard delete 시 일괄 삭제)
- 테이블과 컬럼에 COMMENT 작성
- DML(choimory-dev-member-dml-v1.sql)은 초기 데이터 범위가 정해지면 작성
- 검증: Testcontainers Postgres에 실행하여 오류 없이 적용됨

### 4단계. BaseEntity (TDD)

- @MappedSuperclass, 필드 id, createdAt, modifiedAt, deletedAt
- @EntityListeners(AuditingEntityListener.class)로 createdAt, modifiedAt 자동 채움
- @SQLRestriction("deleted_at IS NULL") 적용
- Persistable<String> 구현. @Transient 플래그로 isNew() 판단, @PostLoad와 @PostPersist에서 false로 변경
- id는 정적 팩토리에서 UuidCreator.getTimeOrderedEpoch()로 채번. JPA용 protected 기본 생성자는 채번하지 않음
- soft delete 메소드로 deletedAt 설정
- 테스트: id가 UUID v7 형식, 새 객체는 isNew true, 로딩 후 false, Auditing 값, soft delete된 행이 엔티티 쿼리에서 제외됨, 네이티브 쿼리로는 조회됨, @MappedSuperclass의 @SQLRestriction이 하위 엔티티에 적용되는지 확인

### 5단계. enum

- MemberStatus: REGISTER, ACTIVATE, INACTIVATE, SUSPENDED
- MemberImageType: PROFILE
- AuthorityCode: MEMBER, ADMIN, MODERATOR
- PermissionAction: CREATE, READ, UPDATE, DELETE
- AgreementType: 값 확인 필요 (확인이 필요한 항목 참고)
- 모두 @Enumerated(EnumType.STRING)으로 매핑

### 6단계. 엔티티 (TDD, 의존 순서대로)

- 공통: private 생성자, 정적 팩토리 create, 수정은 엔티티 내부 메소드, 연관관계는 @ManyToOne(fetch = LAZY) 단방향 기본, 하위 컬렉션은 필요한 곳만 @OneToMany
- follow처럼 개수가 많이 늘어나는 관계는 컬렉션으로 매핑하지 않음 (레포지토리 페이징 조회)
- 전역 설정 hibernate.default_batch_fetch_size
- 순서
  1. member: Member, MemberImage, MemberSuspension, Follow
  2. authority: Authority, Menu(자기참조 parent), Permission, PermissionAction, MemberAuthority
  3. agreement: Agreement, AgreementAnswer, MemberAgreement
- 테스트: 정적 팩토리와 수정 메소드의 동작, 연관관계 저장과 조회

### 7단계. 엔티티 DTO

- 엔티티와 1:1로 매칭. 이름은 엔티티명+Dto (MemberDto 등)
- record + @Builder(toBuilder = true), 엔티티 변환은 DTO의 정적 팩토리 from(Entity)
- 하위 DTO만 포함하고 상위는 제외 (순환 참조 방지). 컬렉션은 stream으로 변환
- 테스트: 필드 매핑, 하위 DTO 포함 여부, 상위 미포함

### 8단계. DDL과 엔티티 정합성 검증

- Testcontainers Postgres에 docs/db의 DDL을 적용한 뒤 spring.jpa.hibernate.ddl-auto=validate로 엔티티 매핑이 DDL과 일치하는지 검증
- 제약 테스트: unique 위반, follow의 자기 팔로우 CHECK 위반, FK ON DELETE CASCADE 동작, 탈퇴 포함 조회가 네이티브로 가능한지

### 9단계. 마무리

- spotlessApply, 전체 빌드와 테스트 통과 확인
- 변경 내용 요약 설명 후 "작업 마무리" 지시에 따라 done1.md 작성, calendar.md DONE 변경, README.md 갱신

## 테스트 계획

- member-domain 테스트는 @DataJpaTest + Testcontainers Postgres로 작성. 테스트 전용 설정 클래스를 test 소스에 둠 (레포지토리는 API 모듈에 있으므로 TestEntityManager 사용)
- 컨테이너 초기화 스크립트로 docs/db/v1의 DDL을 적용함. Gradle 테스트의 작업 디렉토리가 모듈 디렉토리이므로 ../docs/db/v1 상대 경로를 사용함
- Postgres 이미지는 latest 대신 버전을 고정함
- 함수 단위 테스트는 모든 정적 팩토리, 수정 메소드, DTO 변환에 작성함
- API 통합 테스트는 API 구현 시 작성함

## 구현 전 확인이 필요한 항목

1. AgreementType enum 값: DB2에 enum으로만 적혀 있고 값이 정해지지 않음 (예: TERMS, PRIVACY, MARKETING 등)
2. DML 초기 데이터 범위: authority 3종, menu, agreement를 이번에 넣을지 이후 작업으로 미룰지
3. DDL의 스키마명과 id 길이: 옛 schema.sql은 choimory_dev_member 스키마와 varchar(255)를 사용. 스키마명을 유지할지, id를 varchar(36)으로 줄일지
4. 옛 src/main/resources/sql/schema.sql, data.sql과 local 프로필의 sql-init 그룹 처리: 제거할지, docs/db의 DDL을 사용하도록 바꿀지
5. 컴파일되지 않는 RedisConfigTest 처리: 삭제할지, 이후 Redis 설정을 만들 때 재작성할지
6. QueryDSL 설정(Q 클래스 생성은 엔티티가 있는 member-domain에서 수행)을 이번 작업에 포함할지, 레포지토리를 만드는 작업으로 미룰지
7. 테스트용 Postgres 버전
8. Member에 매핑할 하위 컬렉션 범위: member_image, member_suspension, member_agreement, member_authority 중 어느 것을 컬렉션으로 둘지, 그리고 Member DTO가 포함할 하위 DTO 범위
9. 기존 테스트(ChoimoryDevMemberApiApplicationTests)는 ES, Redis 연결 설정값이 필요함. 이 값을 유지할지 (ES, Redis 설정이 제거된 상태라 컨텍스트에서 자동 구성만 동작)

---

# 추가1

- agreement.type 제외 
- dml 제외
- id는 컬럼타입만 uuid로 하고, 채번은 서버에서할거임
- sql init처리는 이번에 만들 ddl, dml로 변경
- redisconfigtest는 내가 미처 못지운 코드임 지우면 됨
- querydsl은 일단 제외해도됨 나중에 할때 추가할거임
- postgres 16인데 다른 버전 필요하면 올려도됨
- member에 매핑할 하위 컬렉션 범위 뭔뜻인지 모르겠네
- es, redis 설정값 어떻게 되어있는데?

--

# 추가1에 대한 의견

> 추가1 확인

## 답변

추가1(180번줄)의 9개 항목에 대한 의견은 아래와 같음. application-datasource.yml과 기존 테스트의 설정값도 함께 확인함.

## agreement.type 제외

type 컬럼을 DB2에서 아예 빼는 것인지, 값(enum 목록)만 나중에 정하는 것인지 확인 필요. 컬럼을 빼면 약관 종류(이용약관, 개인정보, 마케팅)를 title로만 구분하게 되어 나중에 "마케팅 동의 회원만 조회" 같은 쿼리가 불편할 수 있음. 빼는 것이 맞다면 DB2와 어긋나므로 이 문서에 변경으로 기록.

## dml 제외

확정. DML 파일은 비어 있는 채로 둠.

## id는 컬럼 타입만 uuid, 채번은 서버

이 결정으로 달라지는 점이 있음. DB2에서는 id를 varchar로 적었으나 uuid 타입으로 바뀌는 것이므로 변경으로 기록.

- 엔티티의 id 자바 타입은 java.util.UUID로 하는 것을 권장. String id를 uuid 컬럼에 넣으면 Postgres가 타입 불일치 오류를 낼 수 있고, 맞추려면 매핑 어노테이션을 따로 붙여야 함
- Persistable<UUID>, FK 컬럼, DTO의 id 타입도 UUID가 됨. ES나 Redis에 넣을 때는 toString()으로 문자열 변환
- Postgres 16에는 uuidv7() 내장 함수가 없으나 서버에서 채번하므로 문제없음

## sql init을 이번에 만들 ddl, dml로 변경

방향에 동의. 현재 설정에 문제가 있어서 함께 정리해야 함.

- application-local.yml의 spring.sql.init.mode: embedded는 H2 같은 임베디드 DB에서만 스크립트를 실행함. Postgres에서는 현재 설정으로 schema.sql이 실행되지 않음. 실행하려면 always로 변경 필요
- always로 바꾸면 앱이 뜰 때마다 DDL이 실행되므로 DDL을 `create table if not exists`처럼 여러 번 실행해도 안전하게 작성해야 함
- docs/db는 클래스패스에 없으므로 Gradle processResources로 docs/db/v1의 파일을 sql/ 아래로 복사하고 그 경로를 schema-locations로 지정하는 방식을 제안. 멀티 모듈로 바뀌면 실행 디렉토리가 달라져 file: 경로는 불안정함
- 옛 src/main/resources/sql/의 파일은 삭제

## RedisConfigTest 삭제

확정.

## QueryDSL 제외

확정. 계획에서 제외.

## Postgres 16

확정. 테스트 컨테이너도 postgres:16으로 고정.

## "member에 매핑할 하위 컬렉션 범위"의 의미

Member 엔티티가 자기 하위 데이터를 필드로 들고 있을지 정하는 질문.

```java
class Member {
    private String email;
    // 이런 필드를 둘지
    private List<MemberImage> images;
    private List<MemberSuspension> suspensions;
    private List<MemberAuthority> authorities;
    private List<MemberAgreement> agreements;
}
```

- A. 필드를 두지 않음: MemberImage가 member를 가리키기만 하고 회원의 이미지가 필요하면 레포지토리로 따로 조회. MemberDto는 회원 자체 필드만 가짐
- B. 필드를 둠: member.getImages()로 접근하고 MemberDto가 List<MemberImageDto>를 포함

앞서 정한 "객체 연관관계를 철저히 쓰고 DTO도 하위 DTO를 포함"한다는 방침은 B에 해당함. 그러면 질문은 어떤 하위 테이블을 Member의 필드로 둘지. 제안은 images, suspensions, authorities, agreements는 두고, follow는 개수가 많이 늘어나므로 필드로 두지 않는 것. 이렇게 하면 회원 목록 조회 시 하위 데이터까지 읽는 비용이 늘어나는데, 컨벤션이 허용하는 "조회하지 않은 필드는 null" 방식으로 필요한 경우에만 채우면 됨. Authority는 permissions, Permission은 actions, Menu는 children, Agreement는 answers를 둘지도 같은 질문.

## es, redis 설정값

application-datasource.yml은 값을 직접 갖지 않고 자리표시자만 가지고 있음.

- ES: spring.elasticsearch.uris: ${es-host}:${es-port}, username, password
- Redis: host, port, database, username, password, 그리고 spring.cache.type: redis와 TTL 설정
- 실제 값은 환경변수 등 외부에서 주입하는 구조이며 저장소에는 없음
- 기존 컨텍스트 로딩 테스트는 @SpringBootTest(properties = {...})로 localhost:9200, localhost:6379 같은 값을 직접 넣어 자리표시자를 채움. Postgres 연결은 Testcontainers의 @ServiceConnection이 대신함

이번 범위에서 ES/Redis 설정은 제외했지만 의존성과 yml 항목은 남아 있으므로 테스트에는 이 값이 계속 필요함. 그대로 두고 테스트 속성을 유지하는 방식이 가장 단순함.

## 정리

- 확정: dml 제외, sql init을 docs/db의 ddl/dml로 변경(mode: always + 멱등 DDL + processResources 복사), RedisConfigTest 삭제, QueryDSL 제외, Postgres 16, ES/Redis 설정 유지
- 확인 필요: agreement.type을 컬럼째로 뺄지, id 자바 타입을 UUID로 할지, Member 등 하위 컬렉션 필드 범위(A/B)

---

# 추가2

- agreement.type 그냥 테이블에서 컬럼 제외. 나중에 필요할때 추가
- id 자바타입 string할거임. db에 따라 코드가 달라지잖아. 꼭 바꿔야한다면 차라리 그냥 postgres의 id 컬럼을 varchar로 쓸거임
- 엔티티 하위 컬렉션 두고 연관관계 어노테이션도 다 붙일거임. dto에도 하위dto로 필드에 컬렉션 둘거임 
- sql init은 always는 운영에 영향가니까 절대안되고 로컬에서만. 애초에 sql init을 그럴 용도로 붙인거임
- es, redis 설정값 제거하고 나중에 구현할때 다시 추가

--

# 추가2에 대한 의견

> 추가2 확인

## 답변

추가2(279번줄)의 5개 항목에 대한 의견은 아래와 같음.

## 1. agreement.type 컬럼 제외

확정. DB2와 어긋나는 변경이므로 이 문서에 변경으로 기록. 나중에 컬럼을 추가하는 것은 하위 호환 변경(컬럼 추가)이라 부담이 적음.

## 2. id 자바 타입은 String, 꼭 바꿔야 하면 varchar

방향에 동의. 정리는 다음과 같음.

- 자바 타입은 String, DB 컬럼은 varchar(36). DB2의 원래 정의(id varchar)로 돌아가고, 추가1에서 논의한 "컬럼 타입만 uuid"는 취소됨
- String id를 uuid 컬럼에 매핑하려면 Postgres 쪽 매핑이 필요한데 이는 "DB에 따라 코드가 달라지는 것"에 해당하므로 varchar가 맞음
- UUID v7을 문자열로 저장해도 시간 순서가 사전식 정렬과 일치하여 인덱스 효율이 유지됨. 저장 크기는 uuid 타입보다 크지만 감수할 수준
- 나중에 uuid 타입으로 바꾸고 싶으면 마이그레이션으로 처리 가능
- FK 컬럼도 모두 varchar(36)으로 통일

## 3. 엔티티 하위 컬렉션과 연관관계 어노테이션 전부, DTO에도 하위 DTO 컬렉션

추가1에서 설명한 B 방식으로 확정. 구현 전에 정해 두면 좋은 것은 다음과 같음.

- follow도 "다"에 포함되는지: Member가 followers, followees 컬렉션을 갖게 하면 DTO 변환 때 전체를 읽음. plan3에서 정한 대로 follow는 컬렉션으로 매핑하지 않고 레포지토리 페이징 조회를 권장
- cascade 정책: cascade = PERSIST만 두고 REMOVE와 orphanRemoval은 쓰지 않는 것을 권장. 삭제를 soft delete로 처리하는데 orphanRemoval은 실제 DELETE를 날리기 때문. hard delete는 DB의 ON DELETE CASCADE가 담당
- 양방향 관계: 부모에 컬렉션을 두고 자식에 @ManyToOne이 있으면 양방향이므로 mappedBy와 연관관계 편의 메소드가 필요
- 연결 테이블의 DTO 규칙: 컨벤션상 하위 DTO만 포함하고 상위는 제외. MemberAgreementDto가 AgreementAnswerDto를 포함하면 그 답변이 어느 약관인지 알려 줄 상위(Agreement) 정보가 빠짐. 이런 경우 부모의 id를 일반 필드(agreementId)로 둘지 결정 필요. DTO 단계에서 결정
- 자식 soft delete: 자식 엔티티에도 @SQLRestriction이 붙으므로 soft delete된 자식은 컬렉션에서 자동으로 빠짐. 탈퇴 시에는 member만 soft delete하기로 했으므로 자식은 그대로 남음

## 4. sql init은 로컬에서만, 운영에는 절대 안 됨

전적으로 동의. 변경 대상은 application-local.yml에서만 mode: always로 바꾸는 것이고 application-dev.yml과 application-prod.yml은 건드리지 않음. local 프로필이 아닐 때는 실행되지 않음. 기본 프로필이 prod로 설정되어 있어 실수로 프로필 없이 실행해도 안전함. 로컬에서도 DDL은 if not exists로 멱등하게 작성하고 DROP은 넣지 않음. DML을 나중에 넣을 때도 ON CONFLICT DO NOTHING 같은 멱등 처리가 필요.

## 5. ES, Redis 설정값 제거, 나중에 구현할 때 추가

동의. 추가 제안은 yml의 설정값만 지우고 build.gradle의 spring-boot-starter-data-elasticsearch, spring-boot-starter-data-redis를 남겨 두면 자동 구성이 기본값(localhost)으로 동작하여 의도치 않은 연결 시도나 빈이 생길 수 있다는 점. 의존성도 같이 제거하고 나중에 구현할 때 함께 추가하는 편이 깔끔함. 그러면 컨텍스트 테스트의 ES/Redis 속성도 지울 수 있고 JPA와 ES 레포지토리 공존 문제도 지금은 사라짐. 이 방식으로 할지 결정 필요.

## 정리

- 확정: 1, 2(String + varchar(36)), 3, 4, 5(설정값 제거)
- 확인 필요: follow를 컬렉션에서 제외할지, cascade 정책(PERSIST만), ES/Redis 의존성도 제거할지
- 앞쪽 "구현 계획" 섹션에는 이전 결정(uuid 타입, QueryDSL, AgreementType 등)이 남아 있으므로, 결정이 모이면 "최종 구현 계획 변경사항" 섹션으로 정리 예정

---

# 추가3

- Es, redis 의존성은 냅두고 코드만 정리
- db 컬럼 uuid로 하고, 안되면 나중에 varchar(36)으로 변경
- follower, followee도 member 엔티티 필드에 추가. 어차피 객체변환때 변환 대상에서 제외해서 필드접근 안하면 조회 쿼리 시행 안하잖아. 아니면 그냥 member에서 빼고 별도 엔티티객체로만 써야하나?
- cascade는 all. remove랑 orphan remove 안쓰는거 권장하는 이유가 뭔데?

--

# 추가3에 대한 의견

> 추가3 확인

## 답변

추가3(339번줄)의 4개 항목에 대한 의견은 아래와 같음.

## 1. ES, Redis 의존성은 두고 코드만 정리

의존성은 그대로 두고 yml의 설정값과 테스트의 ES/Redis 속성만 제거하는 것으로 정리. 설정이 없으면 Spring이 기본값(localhost)으로 자동 구성하는데 연결은 실제 사용할 때 맺어지므로 컨텍스트 로딩에는 문제없음.

## 2. DB 컬럼은 uuid, 안 되면 나중에 varchar(36)

방향이 앞의 결정(자바 String + varchar)에서 바뀌었는데 이 조합이 실제로 되는지 확인이 필요함. 자바 String id를 Postgres uuid 컬럼에 쓰면 두 군데에서 걸림.

- JDBC 파라미터로 String을 보내면 Postgres가 uuid 컬럼과 타입이 맞지 않는다고 오류를 냄
- ddl-auto=validate 검증에서 Hibernate가 String은 varchar를 기대하는데 DB에서 uuid를 발견했다고 실패

해결 방법은 두 가지로 알고 있음. 하나는 @JdbcTypeCode(SqlTypes.UUID)로 매핑하는 것이고, 다른 하나는 JDBC URL에 stringtype=unspecified를 붙이는 것. 후자는 모든 문자열 파라미터에 영향을 줌. 어느 쪽이 되는지는 확신할 수 없으므로 작은 검증 테스트를 먼저 하는 것을 제안. 계획의 3단계(DDL 작성) 시작 때 String id 엔티티 하나를 uuid 컬럼에 저장, 조회, validate까지 해 보고, 안 되면 varchar(36)으로 가는 방식. 이러면 "나중에 변경"할 필요 없이 바로 결론이 남.

"DB에 따라 코드가 달라지는 것이 싫다"는 원칙과 단순함만 보면 varchar(36)으로 확정하는 쪽이 간단함. uuid 타입의 이점은 저장 크기(16바이트 대 37바이트)와 인덱스 크기이며 현재 규모에서는 체감이 작음. 그래도 uuid를 먼저 시도하려면 위 검증 방식으로 진행.

## 3. follower, followee도 Member의 필드에 추가

이유는 맞음. 지연 로딩 컬렉션은 필드에 접근하지 않으면 쿼리가 나가지 않고 DTO 변환에서 빼면 접근도 없음. 위험은 "실수로 접근하는 경우".

- 컬렉션을 건드리는 코드(getFollowers().size(), Lombok toString, JSON 직렬화, 로그)가 생기면 팔로워 전체를 읽음
- 컬렉션 필드로는 페이징이 되지 않으므로 팔로워 목록은 결국 레포지토리 페이징 쿼리로 조회하게 됨. 컬렉션 필드로 얻는 이점이 거의 없음

"별도 엔티티로만 쓰는" 방식은 Follow가 @ManyToOne으로 follower와 followee를 가리키는 구조이며 연관관계 어노테이션은 여전히 붙음. Member 쪽 컬렉션만 없을 뿐이고 QueryDSL에서 Follow 기준으로 조인하는 데도 지장이 없음. 이 방식을 권장하지만 필드로 두어도 안전하게 쓸 수 있음. 둘 경우 지킬 것은 다음과 같음.

- cascade는 붙이지 않음
- @ToString.Exclude를 붙이고 DTO 변환에서 제외
- 컬렉션 필드는 private으로 두고 getter를 노출하지 않거나 이름으로 위험을 드러냄

## 4. cascade는 all, remove와 orphanRemoval을 권장하지 않는 이유

쓰지 말라고 단정하는 것이 아니라 아래 위험이 있어서 권장했음.

- orphanRemoval: 부모의 컬렉션에서 자식을 remove하면 그 행이 실제로 DELETE됨. 삭제를 deleted_at 갱신(soft delete)으로 처리하는 프로젝트에서는 누군가 "목록에서 빼기"를 하면 soft delete가 아니라 hard delete가 되어 정책이 조용히 깨짐. 가장 위험한 옵션
- CascadeType.REMOVE: repository.delete(member)를 호출하면 하위 엔티티를 전부 읽어서 하나씩 DELETE함. 하위 데이터가 많으면 쿼리가 많이 나가고 느림. deleted_at이 있는 자식은 @SQLRestriction 때문에 읽히지 않아 자바 쪽 cascade에서 빠지고 DB의 ON DELETE CASCADE가 대신 지움. 동작은 하지만 두 경로가 섞임

반대로 ALL로 가도 되는 면도 있음. hard delete를 "회원 정보를 전부 지우는 것"으로 정했으므로 delete(member)가 하위를 같이 지우는 동작이 의도에 맞음. 정리는 다음과 같음.

- orphanRemoval은 쓰지 않기를 강하게 권장
- REMOVE는 쓸 수 있음. 다만 대량 hard delete 배치는 DELETE ... WHERE를 네이티브로 실행해 DB의 ON DELETE CASCADE에 맡기는 편이 빠름
- cascade는 소유 관계에만 붙임. Member → MemberImage, Agreement → AgreementAnswer, Permission → PermissionAction은 부모가 자식을 소유하므로 괜찮음. 반면 MemberAuthority → Authority, MemberAgreement → AgreementAnswer는 다른 엔티티를 참조만 하므로 cascade를 붙이면 안 됨(공유 데이터가 지워질 수 있음)

## 정리

- 확정: 1(의존성 유지, 설정 정리)
- 확인 필요: 2(uuid 시도 방식, 검증 테스트 제안), 3(follow 컬렉션 필드 여부), 4(cascade 범위: orphanRemoval 제외, 소유 관계에만)

---

# 추가4

- 컬럼 java는 String, postgres는 uuid로 만들어서 해보고 안되면 varchar(36)으로 바꾸면 되잖아 
- member엔티티는 follower, followee 빼고 follow 엔티티에만 follower,followee 넣을까
- cascade는 권장대로

--

# 추가4에 대한 의견

> 추가4 확인

## 답변

추가4(406번줄)의 3개 항목에 대한 의견은 아래와 같음.

## 1. 자바는 String, Postgres는 uuid로 해 보고 안 되면 varchar(36)

동의. 아직 데이터가 없는 단계라 바꾸는 비용이 DDL 한 줄과 FK 컬럼 정도라 시도해 볼 만함. 검증은 3단계(DDL 작성) 시작 때 String id 엔티티 하나로 아래를 확인함.

- 저장과 조회, 네이티브 쿼리의 WHERE id = ?
- FK 컬럼 연결, unique 위반
- ddl-auto=validate 통과

통과 방법이 stringtype=unspecified면 JDBC URL에 붙여야 하므로 application-datasource.yml의 URL도 바뀜. 이 경우 모든 문자열 파라미터에 영향을 주므로 영향 범위를 문서에 기록. 하나라도 안 되면 varchar(36)으로 확정.

## 2. Member에서 follower, followee를 빼고 Follow 엔티티에만 두기

확정. Follow가 @ManyToOne으로 follower와 followee를 가리키고 Member에는 컬렉션 필드를 두지 않음. 팔로워 목록은 레포지토리 페이징 쿼리로 조회. DTO 단계에서 정할 점은 FollowDto가 follower, followee를 MemberDto로 포함하면 회원의 하위 컬렉션(이미지 등)까지 같이 로드될 수 있으므로, 이 경우 하위 컬렉션을 비워서(null) 가져오는 부분 DTO를 쓰는 것.

## 3. cascade는 권장대로

orphanRemoval은 어디에도 쓰지 않고 cascade는 소유 관계에만 붙임.

| 부모 → 자식 | cascade | 비고 |
|---|---|---|
| Member → MemberImage, MemberSuspension | ALL | 회원이 소유 |
| Member → MemberAuthority, MemberAgreement | ALL | 연결 행은 회원이 소유 |
| Authority → Permission | ALL | 권한이 소유 |
| Permission → PermissionAction | ALL | 권한이 소유 |
| Agreement → AgreementAnswer | ALL | 약관이 소유 |
| Menu → 하위 Menu | PERSIST, MERGE만 | 메뉴 삭제가 하위 메뉴를 조용히 지우면 위험 |
| MemberAuthority → Authority | 없음 | 참조만 함 |
| MemberAgreement → AgreementAnswer | 없음 | 참조만 함 |
| Permission → Menu | 없음 | 참조만 함 |
| Follow → Member | 없음 | 참조만 함 |

Menu의 부모 FK는 DDL에서 ON DELETE CASCADE 대신 RESTRICT(기본값)로 둠.

## 정리

- 확정: 1(검증 후 결정), 2, 3
- 흩어진 결정을 모은 "최종 구현 계획" 섹션을 이어서 작성

---

# 최종 구현 계획

> 지금내용 plan4에 추가하고 구현계획 작성

앞쪽 "구현 계획" 섹션과 추가1~4를 거치며 달라진 결정을 모아 정리한 최종본. 앞쪽 섹션과 내용이 다르면 이 섹션이 우선함.

## 확정된 결정 사항

### 범위와 이름

- 모듈: member-domain, member-api (plan2의 "최종 모듈명과 패키지 구조" 기준)
- domain 모듈에는 v1이 없음. api는 member/v1/command, member/v1/query 아래에 controller, service, handler, repository, payload(request, response, query)를 둠
- follow는 member 패키지, agreement와 authority는 별도 패키지
  - member: Member, MemberImage, MemberSuspension, Follow
  - agreement: Agreement, AgreementAnswer, MemberAgreement
  - authority: Authority, Permission, PermissionAction, Menu, MemberAuthority
- 이번 범위에서 제외: 컨트롤러, 서비스, 핸들러, 레포지토리, 요청/응답 객체, 인증, Redis/ES 설정, ES Document, Kafka, Flyway, 배치 모듈, QueryDSL, DML 초기 데이터

### DB와 DDL

- docs/db/v1/choimory-dev-member-ddl-v1.sql에 DB2 기준으로 작성. dml 파일은 비워 둠
- agreement 테이블에서 type 컬럼 제외 (필요할 때 추가)
- enum은 서버에서만 처리. DB 컬럼은 varchar, CHECK 제약 없음. 허용 값은 주석으로 기록
- id: 자바는 String, Postgres는 uuid로 시도하고 안 되면 varchar(36). FK 컬럼도 같은 타입으로 통일. 채번은 서버에서 UUID v7
- DDL은 멱등하게 작성(if not exists), DROP은 넣지 않음
- 제약조건
  - member: email, nickname UNIQUE
  - member_agreement: UNIQUE(member_id, agreement_answer_id)
  - follow: UNIQUE(follower_id, followee_id), CHECK(follower_id <> followee_id)
  - permission_action: UNIQUE(permission_id, action)
  - 하위 테이블 FK는 ON DELETE CASCADE, 단 menu의 부모 FK는 RESTRICT
  - 테이블과 컬럼에 COMMENT 작성
- follow도 deleted_at 컬럼을 유지하되 비즈니스 로직에서 hard delete로 처리

### 설정

- sql init은 application-local.yml에서만 mode: always로 변경하고, docs/db의 ddl, dml을 사용. dev, prod는 건드리지 않음
- docs/db/v1의 파일은 Gradle processResources로 sql/ 아래에 복사하여 클래스패스로 읽음. 옛 src/main/resources/sql의 파일은 삭제
- ES, Redis 의존성은 유지하고 application-datasource.yml의 설정값과 테스트의 ES/Redis 속성만 제거
- RedisConfigTest 삭제
- 테스트 Postgres는 16으로 고정

### 엔티티

- BaseEntity: id, createdAt, modifiedAt, deletedAt. @MappedSuperclass, JPA Auditing, @SQLRestriction("deleted_at IS NULL"), Persistable 구현
- id는 정적 팩토리에서 UuidCreator.getTimeOrderedEpoch()로 채번
- 탈퇴 포함 조회(탈퇴철회, hard delete 배치, 가입 중복 확인)는 네이티브 쿼리. 복잡한 쿼리가 필요하면 해당 엔티티만 @Filter(autoEnabled)로 전환
- 연관관계는 객체 연관관계로 전부 매핑(@ManyToOne LAZY), 부모에 하위 컬렉션 필드를 둠. 단 follow는 Member에 컬렉션을 두지 않고 Follow의 @ManyToOne으로만 연결
- 전역 hibernate.default_batch_fetch_size 설정
- DTO: 엔티티와 1:1, record + @Builder(toBuilder = true), from(Entity) 정적 팩토리, 하위 DTO를 컬렉션으로 포함하고 상위는 제외, 변환은 stream
- 연결 테이블 DTO가 상위 정보를 잃는 경우(예: MemberAgreementDto에서 어느 약관인지)는 부모 id를 일반 필드로 둘지 DTO 단계에서 결정

## cascade 정책

- orphanRemoval은 어디에도 쓰지 않음
- cascade는 소유 관계에만 붙임 (위 "추가4에 대한 의견"의 표 참고)

## 단계별 작업

각 단계 시작 전에 설명하고 승인을 받으며 TDD로 진행함. git commit과 push는 직접 진행하므로 단계가 끝날 때마다 커밋하기 좋은 단위를 설명함.

### 0단계. 시작 준비

- calendar.md의 modeling을 TASK로 변경
- RedisConfigTest 삭제, 현재 빌드 상태 확인
- 산출물: 컴파일 가능한 빌드 기준선

### 1단계. Gradle 멀티 모듈 전환

- settings.gradle에 member-domain, member-api 포함
- 루트 build.gradle: Spotless, Palantir 설정을 subprojects에 공통 적용
- member-domain: java-library, Spring Boot BOM, 의존성은 spring-boot-starter-data-jpa(api 스코프), uuid-creator, Lombok, 테스트용 Testcontainers Postgres
- member-api: Spring Boot 플러그인, project(':member-domain') 의존, 기존 starter 의존성 이동(ES, Redis 포함)
- 기존 src를 member-api로 이동
- 검증: ./gradlew build 통과

### 2단계. 패키지 골격, 스캔 설정, 설정 파일 정리

- domain: dev.choimory.member.domain 아래 common, member, agreement, authority 패키지(command/entity, command/dto)
- api: dev.choimory.member.api 아래 config, common, member/v1/command, member/v1/query
- @EntityScan("dev.choimory.member.domain"), @EnableJpaRepositories 설정
- application-datasource.yml의 ES/Redis 설정값 제거, 컨텍스트 테스트의 ES/Redis 속성 제거
- application-local.yml의 sql init을 always로 변경하고 docs/db의 ddl, dml을 사용하도록 설정, processResources 복사 설정
- 검증: 컨텍스트 로딩 테스트 통과

### 3단계. DDL 작성과 id 타입 검증

- 먼저 String id 엔티티 하나로 uuid 컬럼 검증(저장, 조회, 네이티브 쿼리, FK, unique, validate). 안 되면 varchar(36)으로 확정
- 확정된 타입으로 DB2의 모든 테이블 DDL 작성 (제약조건, COMMENT 포함)
- 검증: Testcontainers Postgres 16에 적용되어 오류 없이 실행되고, 두 번 실행해도 오류 없음

### 4단계. BaseEntity

- 테스트 먼저: id가 UUID v7 형식, 새 객체는 isNew true, 로딩 후 false, Auditing 값, soft delete된 행이 엔티티 쿼리에서 제외, 네이티브 쿼리로는 조회됨, @MappedSuperclass의 @SQLRestriction이 하위 엔티티에 적용되는지
- 구현 후 테스트 통과

### 5단계. enum

- MemberStatus(REGISTER, ACTIVATE, INACTIVATE, SUSPENDED), MemberImageType(PROFILE), AuthorityCode(MEMBER, ADMIN, MODERATOR), PermissionAction(CREATE, READ, UPDATE, DELETE)
- @Enumerated(EnumType.STRING)

### 6단계. 엔티티

- 공통: private 생성자, 정적 팩토리 create, 수정은 엔티티 내부 메소드, 하위 컬렉션 매핑, 양방향이면 mappedBy와 연관관계 편의 메소드
- 순서
  1. member 패키지: Member, MemberImage, MemberSuspension, Follow
  2. authority 패키지: Authority, Menu, Permission, PermissionAction, MemberAuthority
  3. agreement 패키지: Agreement, AgreementAnswer, MemberAgreement
- 테스트: 정적 팩토리와 수정 메소드, 연관관계 저장과 조회, cascade 동작

### 7단계. 엔티티 DTO

- MemberDto 등 엔티티명+Dto, 하위 DTO를 컬렉션으로 포함하고 상위는 제외
- 테스트: 필드 매핑, 하위 DTO 포함, 상위 미포함, 부분 조회 시 null 처리

### 8단계. DDL과 엔티티 정합성 검증

- DDL을 적용한 Postgres에서 spring.jpa.hibernate.ddl-auto=validate로 모든 엔티티 매핑 검증
- 제약 테스트: unique 위반, follow의 자기 팔로우 CHECK 위반, FK ON DELETE CASCADE 동작, menu 부모 FK RESTRICT, 탈퇴 포함 조회가 네이티브로 가능한지

### 9단계. 마무리

- spotlessApply, 전체 빌드와 테스트 통과 확인
- 변경 내용 요약 설명 후 "작업 마무리" 지시에 따라 done1.md 작성, calendar.md DONE 변경, README.md 갱신(README는 삭제 전 구조를 설명하고 있어 전면 갱신 필요)

## 남은 검증 항목

- Postgres uuid 컬럼에 String id를 쓰는 방법과 통과 여부 (3단계)
- @MappedSuperclass에 붙인 @SQLRestriction의 상속 여부 (4단계)
- 연결 테이블 DTO가 상위 정보를 잃는 경우의 처리 (7단계)