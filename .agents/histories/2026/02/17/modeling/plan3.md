# 목차

- [개요](#개요)
- [Persistable이란](#persistable이란)
- [엔티티 구현 전 결정 사항](#엔티티-구현-전-결정-사항)
- [추가1](#추가1)
- [추가1에 대한 의견](#추가1에-대한-의견)
- [추가2](#추가2)
- [추가2에 대한 의견](#추가2에-대한-의견)
- [SQLRestriction을 쓰지 않을 때의 조회 방식](#sqlrestriction을-쓰지-않을-때의-조회-방식)
- [SQLRestriction과 복잡한 탈퇴 회원 쿼리](#sqlrestriction과-복잡한-탈퇴-회원-쿼리)
- [SQLRestriction 유지 결정](#sqlrestriction-유지-결정)

---

# 개요

- plan1에서 회원 DB 설계를 마무리함 (plan1.md의 DB2 섹션 참고)
- plan2에서 모듈명과 패키지 구조를 확정함 (plan2.md의 "최종 모듈명과 패키지 구조" 섹션 참고)
- 이번 plan3에서는 엔티티 구현 방식을 정함

---

# Persistable이란

> persistable이 뭐야?

## 답변

Persistable은 Spring Data의 인터페이스로, "이 엔티티가 새로 만든 것인가?"를 엔티티가 직접 알려주게 하는 장치.

## 왜 필요한가

Spring Data JPA의 save()는 내부에서 엔티티가 새것인지를 판단함.

- 새것이면 persist(INSERT)
- 아니면 merge. 이때 DB에서 먼저 SELECT로 읽어 본 뒤 INSERT나 UPDATE를 함

기본 판단 기준은 ID가 null인지. ID가 null이면 새것, 값이 있으면 이미 있는 것으로 봄.

## 이 프로젝트에서의 문제

ID(UUID v7)를 비즈니스 로직에서 미리 만들어서 넣음. 그러면 새 엔티티인데도 ID가 이미 있으므로 Spring Data가 "기존 엔티티"로 오해해 merge를 하고, INSERT 전에 SELECT 쿼리가 한 번 더 나감. 기능은 동작하지만 저장할 때마다 불필요한 쿼리가 늘어남.

## 해결: Persistable 구현

```java
public interface Persistable<ID> {
    ID getId();
    boolean isNew();   // true면 INSERT, false면 merge
}
```

BaseEntity가 이 인터페이스를 구현하고 isNew()가 "방금 만든 것인가"를 알려주면 됨. 보통 아래 방식으로 판단함.

- 엔티티를 새로 만들 때만 true가 되는 플래그를 두고, 저장 후나 DB에서 읽어 올 때 false로 바꿈(@PostPersist, @PostLoad)
- 또는 created_at이 null인지로 판단

판단 방식은 엔티티 설계 때 결정.

## 이 구조의 이점

BaseEntity 한 곳에서 처리하면 모든 엔티티가 자동으로 적용받음. 개별 엔티티는 신경 쓸 필요가 없음.

---

# 엔티티 구현 전 결정 사항

> 엔티티는 설계할거 없고 plan1의 db 설계한거보고 만들면 될듯?

## 답변

테이블 구조는 plan1.md의 DB2에 다 있으므로 엔티티 클래스를 새로 설계할 것은 없음. 다만 DB2에 없는 엔티티 코드 작성 방식에서 정해야 할 것이 있음. 정하지 않으면 구현하면서 임의로 정하게 되므로, 기본값을 같이 적어 아닌 것만 바꾸는 방식으로 진행.

## 정해야 할 것

1. BaseEntity 공통 처리
   - ID 채번은 `UuidCreator.getTimeOrderedEpoch()`로 엔티티 생성 시 만듦. Persistable.isNew()는 "저장 전이면 true"인 플래그 방식
   - created_at, modified_at은 JPA Auditing(@CreatedDate, @LastModifiedDate)으로 채움
   - soft delete는 deleted_at 컬럼을 두고, 조회 시 자동으로 걸러내는 방식으로 @SQLRestriction("deleted_at IS NULL")을 쓸지 결정 필요. 탈퇴철회가 deleted_at을 null로 되돌리는 방식이므로 탈퇴한 회원을 복구하려면 필터를 우회해서 조회해야 함. 따라서 @SQLRestriction을 자동 적용하지 않고 레포지토리 메소드에서 deletedAtIsNull을 명시하는 방식이 더 안전할 수 있음
2. 연관관계 매핑: @ManyToOne 같은 객체 연관관계를 쓸지, member_id 같은 ID 값만 들고 있을지 결정 필요. 컨벤션은 하위 엔티티가 상위를 포함해 변환하지 않는다는 규칙이 있으므로 연관관계를 쓰는 것으로 보임. 기본값은 지연 로딩(LAZY) 단방향
3. enum 매핑: DB enum으로 가기로 했는데 Postgres에서는 CREATE TYPE이 필요하고 Hibernate에서는 @JdbcTypeCode(SqlTypes.NAMED_ENUM) 같은 매핑이 필요함. 단순히 @Enumerated(EnumType.STRING)만 쓰면 Postgres enum 타입과 맞지 않음. 이 방식으로 갈지 확인 필요
4. 스키마 관리: DDL을 직접 SQL로 작성해 관리할지(Flyway 등), Hibernate가 자동 생성하게 할지 결정 필요. 하위 호환 스키마 운영을 하려면 마이그레이션 도구를 권장
5. follow의 deleted_at: hard delete 테이블이라 BaseEntity를 상속하면서 이 컬럼을 어떻게 처리할지 결정 필요
6. DB2에서 합의되지 않은 UNIQUE와 agreement의 version, is_required: 현재는 DB2 그대로 가기로 한 상태

## 진행 방식 제안

- 엔티티를 만드는 일은 구현 작업이므로 규칙대로 calendar.md를 TASK로 바꾸고 시작하는 단계
- 위 1~5번은 짧게 정리하고 결정한 다음에 구현에 들어가는 것이 안전

---

# 추가1

1. id채번 그렇게 하면 엔티티 새 객체 생성시에만 채번되는건가? audit 사용할거임. @sqlrestriction 사용할거임. 별도 쿼리칠땐 querydsl로 직접 칠거임.
2. 객체 연관관계 철저히 쓸거임. dto도 id필드 아니고 실제 dto객체 포함관계로 작성할거임. 단 순환참조 고려해서 하위dto만 포함할거고 상위는 제외.
3. postgres는 varchar로 가자 그럼
4. docs/db/{version} 보면 sql파일 있어. 거기에 관리할거야. 그리고 이거 flyway에도 사용할수 있나? 검토도
5. 비즈니스로직으로 직접 hard delete 하면 되지 않나?
6. db2대로

--

# 추가1에 대한 의견

> plan3 87번줄 확인

## 답변

87번줄은 `# 추가1`이며, 6개 항목에 대한 의견은 아래와 같음. docs/db와 application.yml도 함께 확인함.

## 1. ID 채번, Audit, @SQLRestriction, QueryDSL

- 채번 시점: 엔티티의 정적 팩토리(create)에서만 채번하게 하면 새 객체를 만들 때만 생성됨. JPA가 DB에서 읽을 때는 protected 기본 생성자를 쓰므로 채번이 일어나지 않음. 필드 초기값으로 채번하면 읽을 때마다 UUID를 만들고 버리게 되므로 피해야 함
- Audit: 동의. @EnableJpaAuditing과 BaseEntity의 @EntityListeners(AuditingEntityListener.class) 조합으로 처리
- @SQLRestriction: 결정에 동의하나 우회할 수 없다는 점을 알고 가야 함. 이 필터는 JPQL과 QueryDSL을 포함한 모든 엔티티 쿼리에 붙고 끌 수 없음. 아래 경우가 문제가 됨
  - 탈퇴철회: deleted_at이 있는 회원을 찾아서 복구해야 함
  - hard delete 배치: 탈퇴 후 기간이 지난 회원을 찾아야 함
  - 가입 시 이메일/닉네임 중복 확인: unique 제약은 soft delete된 행까지 포함하는데 엔티티 쿼리로는 그런 행이 보이지 않아 INSERT 때 제약 위반 오류가 남

  이런 경우에는 네이티브 쿼리(또는 JdbcTemplate)를 쓰거나, 필터를 켜고 끌 수 있는 Hibernate @Filter를 쓰는 방법이 있음. "별도 쿼리는 QueryDSL"이라고 했으나 이 세 경우만은 QueryDSL(JPQL 기반)로 안 되므로 예외로 두면 됨. 세 경우를 문서에 명시해 두는 것을 권장

## 2. 객체 연관관계와 중첩 DTO

방향에 동의. 챙길 점은 다음과 같음.

- 하위 DTO만 포함하면 N+1이 생기기 쉬움. 목록 조회에서는 fetch join이나 QueryDSL로 한 번에 읽어야 함
- open-in-view는 application.yml에서 이미 false라서 좋음. DTO 변환은 트랜잭션 안(Handler)에서 해야 지연 로딩이 동작함

N:N 테이블(MemberAgreement 등)에서 "상위/하위" 방향은 구현할 때 테이블마다 정하면 됨.

## 3. "postgres는 varchar로 가자"

Postgres 네이티브 enum 대신 varchar + 서버 enum(@Enumerated(EnumType.STRING))으로 가자는 뜻으로 이해함. 확인 필요. 이 경우 plan1.md의 추가논의4 3번에서 정한 "db+서버 모두 enum"이 바뀌는 것이므로 문서에 변경으로 기록해야 함. DB 쪽 방어를 유지하려면 `CHECK (status IN (...))` 제약을 거는 방법이 있음. Postgres에서는 네이티브 enum보다 매핑이 훨씬 단순함.

## 4. docs/db/{version} SQL과 Flyway

- 현재 상태: docs/db/1.0.0/에 choimory-dev-member-ddl-1.0.0.sql과 choimory-dev-member-dml-1.0.0.sql이 있으나 둘 다 비어 있음. ddl-auto: none, generate-ddl: false가 이미 설정되어 있으므로 DDL을 SQL로 관리하는 방향이 이미 잡혀 있음
- Flyway를 같은 파일로 쓸 수 있음. 조건은 다음과 같음
  - 파일 이름이 `V1.0.0__member_ddl.sql`처럼 `V{버전}__{설명}.sql` 규칙이어야 함. 현재 이름은 그대로는 인식되지 않으므로 변경 필요
  - 적용된 마이그레이션 파일은 수정하면 안 됨(체크섬 검증). 변경은 새 버전 파일(V1.0.1__...)로 추가. 앞서 정한 하위 호환 스키마 변경(expand/contract)과 잘 맞음
  - 시드 데이터(권한, 메뉴, 약관 기본값)인 dml은 같은 방식의 버전 파일이나 반복 실행용 R__ 파일로 관리 가능
  - jar로 배포하려면 SQL이 클래스패스에 있어야 함. docs/db를 그대로 두고 Gradle processResources로 db/migration 아래에 복사하면 됨(Flyway는 하위 폴더도 읽음)
  - flyway-core와 Postgres용 flyway-database-postgresql 의존성이 필요하고 마이그레이션은 member-api가 실행하게 함
- 테스트(Testcontainers)에도 같은 파일이 자동으로 적용되어 운영과 테스트 스키마가 일치함

권장: Flyway를 사용하고 파일 이름 규칙만 맞춤.

## 5. follow는 비즈니스 로직으로 hard delete

repository.delete(...)로 지우면 됨. 실제로 남은 질문은 follow 테이블에 쓰지 않는 deleted_at 컬럼을 둘지 여부.

- BaseEntity 하나에 모든 공통 컬럼을 두고 follow도 그대로 상속(단순하지만 안 쓰는 컬럼이 생김)
- BaseEntity(id, created_at, modified_at)와 SoftDeleteEntity(+ deleted_at, @SQLRestriction)로 나누고 follow는 앞의 것만 상속

두 번째를 권장. 안 쓰는 컬럼이 없고 hard delete 대상이 구조로 드러남. 다만 DB2의 "모든 테이블 공통 컬럼" 규칙에서 follow가 예외가 되므로 문서에 기록해야 함.

## 6. DB2대로

확정.

## 정리

- 확정: 1(채번, Audit, @SQLRestriction), 2, 6
- 확인 필요: 3의 해석(varchar + CHECK), 5의 BaseEntity 분리 여부, @SQLRestriction 우회 케이스 3가지 처리 방식, Flyway 도입 여부와 파일 이름 변경

---

# 추가2

1. db마다 enum 활용이 다르면 그냥 db는 냅두고 server로만 푸는게 맞음. db에 따라 코드가 달라지는거 별로니까 enum은 그냥 server로만 풀자
2. sqlrestriction에 querydsl 쓴다는건 탈퇴회원 찾을때 deleted_at is not null 조건 쿼리 쳐서 쓴다는거거임. sqlrestriction 쓰면 됨
3. flyway 파일은 나중에 필요할때 만들자. 일단 스키마 정의할 sql파일은 docs/db에 관리할거임
4. follow도 deleted_at 컬럼 둘거임. 공통사항이니까 다 가져갈거임. 대신 비즈니스로직에서 hard delete로 처리할거임
5. 엔티티조회는 batchsize쓸거고 dto변환은 정적팩토리메소드에서 직접 변환로직 stream으로 작성할거임

--

# 추가2에 대한 의견

> 165번줄 확인

## 답변

165번줄은 `# 추가2`이며, 5개 항목에 대한 의견은 아래와 같음. 2번은 그대로 두면 동작하지 않는 부분이 있음.

## 2. @SQLRestriction을 쓰면서 QueryDSL로 deleted_at IS NOT NULL 조회

이 방식으로는 탈퇴 회원을 찾을 수 없음. @SQLRestriction("deleted_at IS NULL")은 그 엔티티의 모든 엔티티 쿼리에 deleted_at IS NULL을 자동으로 붙이므로, QueryDSL에서 deleted_at IS NOT NULL을 넣으면 실제 실행되는 조건은 `deleted_at IS NOT NULL AND deleted_at IS NULL`이 되어 항상 빈 결과. 쿼리 작성 방식은 상관없고 엔티티 쿼리인 이상 필터를 피할 수 없음. 선택지는 다음과 같음.

- 네이티브 쿼리나 JdbcTemplate (권장): 탈퇴철회, hard delete 배치, 가입 중복 확인처럼 필요한 곳이 몇 군데뿐이므로 이 세 경우만 네이티브로 처리
- @Filter 방식: 켜고 끌 수 있어 우회는 되나 모든 조회에 필터를 켜는 설정이 따로 필요
- 같은 테이블을 매핑하는 읽기 전용 엔티티를 하나 더 두는 방식: 쓸 수는 있으나 클래스가 중복됨

구현할 때 테스트 한 개로 이 동작을 확인해 두면 확실함.

## 1. enum은 서버에서만

동의. DB 종류에 따라 코드가 달라지는 것을 피한다는 이유가 타당함. 정리는 다음과 같음.

- DB 컬럼은 varchar, 엔티티는 @Enumerated(EnumType.STRING). CHECK 제약도 두지 않음
- 이전의 "DB와 서버 모두 enum"(plan1.md 추가논의4 3번)과 DB2의 해당 줄은 이 결정으로 대체됨. 문서에 변경으로 기록
- DDL에는 허용 값을 주석으로 남겨 두면 나중에 편함

## 3. Flyway는 나중에, 스키마 SQL은 docs/db에

동의. 참고 사항은 다음과 같음.

- 지금부터 파일 이름을 `V1.0.0__member_ddl.sql` 같은 Flyway 규칙으로 지으면 나중에 도입할 때 이름을 바꿀 필요가 없음 (선택)
- Flyway와 ddl-auto가 모두 없으면 테스트(Testcontainers)에서 스키마를 어떻게 올릴지 정해야 함. 컨테이너에 docs/db의 SQL을 초기화 스크립트로 연결하는 방식이 가장 간단

## 4. follow도 deleted_at을 두고 hard delete

동의. BaseEntity를 하나로 유지해 가장 단순함. deleted_at은 쓰지 않는 컬럼이 되고, @SQLRestriction이 걸려도 값이 항상 null이라 영향이 없음. @MappedSuperclass에 붙인 @SQLRestriction이 하위 엔티티에 적용되는지는 구현할 때 테스트로 확인.

## 5. 엔티티 조회는 batch size, DTO 변환은 정적 팩토리에서 stream

동의. hibernate.default_batch_fetch_size를 전역으로 설정하면 지연 로딩 때 IN 쿼리로 묶어 읽어 N+1을 크게 줄임. 주의할 점은 다음과 같음.

- DTO 변환은 트랜잭션 안에서 해야 함 (Handler가 이 위치)
- follow처럼 개수가 많이 늘어나는 관계는 컬렉션으로 매핑하지 않는 것을 권장. Member.followers 같은 컬렉션을 DTO로 변환하면 전부 읽게 됨. 이런 것은 레포지토리에서 페이징 쿼리로 가져옴

## 정리

- 확정: 1, 3, 4, 5
- 수정이 필요한 것: 2번(@SQLRestriction과 탈퇴회원 조회). 처리 방식 결정 필요

--

# SQLRestriction을 쓰지 않을 때의 조회 방식

> 그럼 sqlrestriction을 안쓰면 쿼리마다 is null 붙여서 조회해야해?

## 답변

기본적으로는 그러함. @SQLRestriction이 하는 일이 바로 그 조건을 자동으로 붙여 주는 것이므로 쓰지 않으면 조회마다 deleted_at IS NULL을 직접 넣어야 함. 다만 실수를 줄이는 방법이 있음.

## 1. 안 쓸 때의 방식과 약점

- Spring Data 메소드 이름에 AndDeletedAtIsNull을 붙이거나 QueryDSL에서 deletedAt.isNull()을 넣음
- 약점은 하나라도 빼먹으면 탈퇴한 데이터가 노출된다는 것. 연관관계로 읽는 하위 데이터(member.getImages())나 조인에서도 빠지기 쉬움. 사람이 기억해서 지키는 규칙이라 코드가 늘수록 위험해짐

## 2. 실수를 줄이는 방법

- 공통 조건 메소드로 모으기: QueryDSL이면 isActive() 같은 조건 메소드를 한 곳에 만들어 모든 쿼리에서 재사용. 빼먹을 확률이 줄지만 강제되지는 않음
- Hibernate @Filter: 기본으로 켜 두고 필요한 곳(탈퇴철회, 배치)에서만 끔. @SQLRestriction과 비슷하게 자동 적용되면서 우회도 가능. 대신 매 요청이나 트랜잭션마다 필터를 켜는 설정이 필요하다고 설명했으나, 이후 답변에서 autoEnabled로 보완됨(아래 섹션 참고)

## 3. 비교

| | 자동 적용 안전성 | 우회 필요한 경우 |
|---|---|---|
| @SQLRestriction | 가장 높음 (빼먹을 수 없음) | 네이티브 쿼리 필요 |
| @Filter | 높음 (켜는 설정만 맞으면) | 필터 끄기로 가능 |
| 수동 is null | 낮음 (사람이 챙김) | 조건을 안 넣으면 됨 |

## 의견

@SQLRestriction을 그대로 쓰는 쪽을 권장. 우회가 필요한 곳은 탈퇴철회, hard delete 배치, 가입 중복 확인 세 군데뿐이므로 그 부분만 네이티브 쿼리로 처리하면 됨. 나머지 모든 조회에서 노출 사고를 구조적으로 막아 주는 이점이 더 큼. 소프트 삭제 데이터가 실수로 노출되는 것은 개인정보와 관련된 사고이므로 안전한 쪽을 기본값으로 두는 것이 나음. 수동 방식은 우회 지점이 많고 is null 누락을 코드 리뷰와 테스트로 확실히 잡을 수 있을 때나 고려할 만함.

--

# SQLRestriction과 복잡한 탈퇴 회원 쿼리

> 하나 걸리는건 sqlrestriction을 썼을때 좀 복잡한 쿼리라서 네이티브쿼리로 처리하기 어려운 케이스가 좀 걱정됨. 이때 querydsl을 사용해야하는데 querydsl로는 안된다고 하니까

## 답변

걱정하는 부분은 맞음. 그리고 @Filter에 대해 앞서 한 말을 수정함. "모든 조회에 필터를 켜는 설정이 따로 필요하다"고 했으나 Hibernate 6.5 이상에서는 @FilterDef(autoEnabled = true)로 세션마다 자동으로 켜지게 할 수 있는 것으로 알고 있음(Spring Boot 3.5의 Hibernate가 해당 범위. 구현 때 확인 필요).

## @Filter(autoEnabled)로 가는 경우

- 기본은 @SQLRestriction처럼 모든 엔티티 쿼리에 deleted_at IS NULL이 자동으로 붙음
- 탈퇴 회원이 필요한 쿼리에서만 해당 세션의 필터를 끄고 QueryDSL로 deletedAt.isNotNull() 조건을 넣으면 됨. 복잡한 쿼리도 QueryDSL 그대로 쓸 수 있어 네이티브로 옮길 필요가 없음
- 약점은 @SQLRestriction보다 적용 범위가 좁다는 것. 기본 설정으로는 id로 직접 조회(findById, getReference)나 일부 연관 로딩에서 필터가 안 붙을 수 있음(applyToLoadByKey 같은 옵션 확인 필요). 그래서 "빼먹을 수 없다"는 @SQLRestriction의 장점이 약해짐

## 실제로 필요한 경우가 얼마나 되는지

탈퇴 데이터를 읽어야 하는 곳은 지금까지 아래 세 가지.

- 탈퇴철회: id로 한 건 찾아서 deleted_at을 null로 갱신하는 단순 쿼리
- hard delete 배치: "deleted_at이 N일 이전인 회원 id 목록"을 읽는 단순 쿼리
- 가입 중복 확인: email 또는 nickname으로 exists를 조회하는 단순 쿼리

모두 단순해서 네이티브로 충분함. 복잡한 쿼리가 걱정이라면 어떤 경우인지가 중요한데, 예를 들어 관리자가 탈퇴 회원을 조건 검색하는 화면 같은 경우. 그런 검색 화면은 CQRS 구조에서 ES 조회 쪽이 맡을 가능성이 크고, 그러면 Postgres에서 복잡하게 조인할 일이 줄어듦.

## 선택지 정리

| 방식 | 복잡한 쿼리 | 안전성 | 비용 |
|---|---|---|---|
| @SQLRestriction + 네이티브 | 네이티브로 직접 작성 | 가장 높음 | 낮음 (복잡한 경우에만 부담) |
| @SQLRestriction + QueryDSL SQL(querydsl-sql) | 타입 안전한 SQL 빌더로 작성 가능 | 가장 높음 | 의존성과 Q타입 생성 설정 추가 |
| @Filter (autoEnabled) | QueryDSL(JPA) 그대로 | 높음 (id 조회 등 범위 확인 필요) | 설정 한 번, 동작 검증 필요 |
| 우회용 읽기 전용 엔티티 | QueryDSL 가능 | 높음 | 클래스 중복. 연관관계 방향에 따라 필터가 걸려 복잡 |

## 의견

- 지금 알려진 경우는 전부 단순하므로 @SQLRestriction으로 시작하고 해당 세 곳만 네이티브로 처리하는 것이 가장 안전하고 단순함
- 나중에 정말 복잡한 "탈퇴 포함" 쿼리가 생기면 그때 querydsl-sql이나 @Filter로 옮겨도 늦지 않음. 엔티티 매핑 어노테이션 하나를 바꾸는 정도라 비용이 크지 않음
- 그래도 지금 걱정이 크면 @Filter 방식으로 가고 구현 시작 단계에서 "필터 기본 적용, findById, 연관 로딩, 필터 끄기" 네 가지를 테스트로 확인하는 것을 권장

--

# SQLRestriction 유지 결정

> @SQLRestriction 유지하고 필요할때는 네이티브 쿼리나, querydsl+filter쓰는걸로 가자

## 답변

확정. 다만 "querydsl + filter" 부분은 그대로는 동작하지 않아 정리가 필요함.

## 문제: @SQLRestriction이 붙은 엔티티에서는 @Filter를 꺼도 소용없음

@Filter는 필터를 끄면 조건이 빠지지만 @SQLRestriction은 끌 수 없는 고정 조건. 같은 엔티티에 둘을 같이 붙여도 @SQLRestriction의 deleted_at IS NULL은 계속 적용되므로 QueryDSL로 탈퇴 회원을 조회하면 여전히 빈 결과. 따라서 아래처럼 정리함.

- 엔티티마다 둘 중 하나만 붙임. @SQLRestriction이 붙은 엔티티의 탈퇴 포함 조회는 네이티브 쿼리로 처리
- QueryDSL로 탈퇴 포함 조회가 꼭 필요한 엔티티가 생기면 그 엔티티만 @SQLRestriction을 빼고 @Filter(autoEnabled)로 변경

## BaseEntity에 붙이는 위치

@SQLRestriction을 BaseEntity(공통 부모)에 붙이면 모든 엔티티가 상속하는데, 나중에 특정 엔티티만 @Filter로 바꾸려 해도 상속받은 @SQLRestriction을 뺄 수 없음. 선택지는 두 가지.

1. 지금은 BaseEntity에 붙이고, 바꿔야 할 때 BaseEntity에서 빼서 엔티티마다 붙이는 방식으로 옮김. 지금 가장 단순하고 안전함
2. 처음부터 엔티티마다 붙임. 유연하지만 새 엔티티에 빼먹을 위험이 있음

1번을 권장. 현재 알려진 탈퇴 포함 조회가 전부 단순하고 네이티브로 충분하므로, 바꾸는 시점이 올 때까지는 걱정하지 않아도 됨.

## 확정 내용 정리

- @SQLRestriction("deleted_at IS NULL")을 BaseEntity에 붙이고 유지
- 탈퇴 포함 조회(탈퇴철회, hard delete 배치, 가입 중복 확인)는 네이티브 쿼리로 처리
- 복잡한 탈퇴 포함 쿼리가 생기면 해당 엔티티만 @Filter(autoEnabled) + QueryDSL로 전환. 이때 그 엔티티의 @SQLRestriction은 제거해야 함
- 구현 시작 단계에서 @MappedSuperclass에 붙인 @SQLRestriction이 하위 엔티티에 실제로 적용되는지 테스트로 확인