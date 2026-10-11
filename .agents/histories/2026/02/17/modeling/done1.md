# 목차

- [개요](#개요)
- [결과 요약](#결과-요약)
- [주요 결정 사항](#주요-결정-사항)
- [단계별 결과](#단계별-결과)
- [검증 결과](#검증-결과)
- [남은 일](#남은-일)

---

# 개요

- 회원 도메인의 DB 설계(plan1)와 엔티티/프로젝트 구조 설계(plan2~plan4)를 바탕으로, 멀티 모듈 전환부터 DDL, 엔티티, DTO, 정합성 검증까지 TDD로 구현한 작업의 완료 요약
- 상세한 진행 기록과 논의는 plan1~plan4.md, task1.md 참고
- git commit과 push는 직접 진행하므로 수행하지 않았음

---

# 결과 요약

- Gradle 멀티 모듈 구성: `member-domain`(엔티티, DTO, enum), `member-api`(Spring Boot 애플리케이션)
- DDL(`docs/db/v1/choimory-dev-member-ddl-v1.sql`): 12개 테이블, 공통 컬럼(id, created_at, modified_at, deleted_at), unique/check/FK 규칙, 멱등 실행 가능(`create ... if not exists`), COMMENT ON 포함
- 엔티티 12개(Member, MemberImage, MemberSuspension, Follow, Authority, Menu, Permission, PermissionAction, MemberAuthority, Agreement, AgreementAnswer, MemberAgreement)와 BaseEntity
- 엔티티 DTO 12개(record), enum 4개(MemberStatus, MemberImageType, AuthorityCode, PermissionActionType)
- 테스트 110건 통과 (member-domain 98건, member-api 12건)

---

# 주요 결정 사항

- id: Java `String`, DB `varchar(36)`, UUID v7(`UuidCreator.getTimeOrderedEpoch()`)을 BaseEntity 생성자에서 채번
- `Persistable<String>`과 `@Transient` 신규 여부 플래그로 id를 미리 채번해도 save() 시 불필요한 SELECT가 나가지 않도록 함
- Auditing: `@EnableJpaAuditing(modifyOnCreate = false)`로 수정 이력이 없으면 modified_at은 null
- Soft delete: 각 엔티티에 `@SQLRestriction("deleted_at IS NULL")` 선언 (BaseEntity에서 상속되지 않음), 삭제된 행은 네이티브 쿼리로 조회, follow는 hard delete
- cascade: 소유하는 자식은 ALL(orphanRemoval 없음), 참조만 하는 관계는 없음, Menu 하위 메뉴는 PERSIST/MERGE만. DB FK는 소유 관계 CASCADE, 참조 관계 RESTRICT
- 엔티티 생성: 정적 팩토리 없이 클래스 레벨 `@Builder`, private `@AllArgsConstructor`, protected `@NoArgsConstructor`, `@Table` 미사용. 부모 컬렉션 등록은 부모의 `add*` 메소드 호출. 자기 팔로우 검증은 Service 책임이며 DB CHECK가 최종 방어선
- DTO: record + `@Builder(toBuilder = true)` + 정적 `from(Entity)`, 자식 DTO에는 부모 객체와 부모 id 없음, 참조만 하는 관계는 `fromWithoutChildren`
- 제외: QueryDSL, Flyway, Kafka, ES Document, AgreementType, DML 데이터 (ES/Redis 의존성은 member-api에 유지)

---

# 단계별 결과

| 단계 | 내용 | 상태 |
|---|---|---|
| 0 | 시작 준비 (calendar TASK, RedisConfigTest 삭제) | 완료 |
| 1 | Gradle 멀티 모듈 전환 | 완료 |
| 2 | 패키지 골격, 스캔 설정, 설정 파일 정리 | 완료 |
| 3 | DDL 작성과 id 타입 검증 | 완료 |
| 4 | BaseEntity | 완료 |
| 5 | enum | 완료 |
| 6 | 엔티티 12개와 생성 방식 정리 | 완료 |
| 7 | 엔티티 DTO 12개 | 완료 |
| 8 | DDL과 엔티티 정합성 검증 | 완료 |
| 9 | 최종 점검 | 완료 |
| 후속 | 엔티티 create() 제거, 클래스 레벨 Builder 전환 | 완료 |

---

# 검증 결과

- `./gradlew spotlessApply` 후 `./gradlew clean build` 통과 (spotlessCheck 포함)
- 테스트 110건 모두 통과, 건너뛴 테스트 없음
- Testcontainers Postgres 16에 DDL을 적용하고 `ddl-auto=validate`로 엔티티와 DDL 정합성 확인

---

# 남은 일

- 레포지토리, 서비스, 컨트롤러 등 member-api 구현 (ES Document, Redis 설정 포함)
- 자기 팔로우 검증 등 엔티티에서 뺀 비즈니스 검증을 Service에 구현
- DDL의 COMMENT ON을 각 create table 아래로 옮길지 결정
- 코드 컨벤션 문서(code-convention-springboot.md)의 Entity 예시를 최종 생성 방식에 맞출지 결정 (수정하지 않았음)
- plan1~plan4.md의 기존 본문에는 이후 바뀐 결정(uuid 타입, QueryDSL, AgreementType 등)이 남아 있음 (규칙상 수정하지 않았고 후반부에 변경 경위 기록)
