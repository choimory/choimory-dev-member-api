# 목차

- [개요](#개요)
- [개요에 대한 의견](#개요에-대한-의견)
- [추가1](#추가1)
- [추가1에 대한 의견](#추가1에-대한-의견)
- [추가2](#추가2)
- [추가2에 대한 의견](#추가2에-대한-의견)
- [추가3](#추가3)
- [추가3에 대한 의견](#추가3에-대한-의견)
- [추가4](#추가4)
- [추가4에 대한 의견](#추가4에-대한-의견)
- [추가 5](#추가-5)
- [추가 5에 대한 의견](#추가-5에-대한-의견)
- [추가6](#추가6)
- [추가6에 대한 의견](#추가6에-대한-의견)
- [테이블 변경과 하위 버전 API](#테이블-변경과-하위-버전-api)
- [스키마 하위 호환 운영 방식](#스키마-하위-호환-운영-방식)
- [model 모듈 v1 제거와 model/domain 용어](#model-모듈-v1-제거와-modeldomain-용어)
- [최종 모듈명과 패키지 구조](#최종-모듈명과-패키지-구조)

---

# 개요

- plan1에서 회원 DB 설계를 마무리함 (plan1.md의 DB2 섹션 참고)
- 이번 plan2에서는 프로젝트 구조를 설계함
- gradle 멀티 모듈 프로젝트로 만들고 싶음 (향후 만들 스프링 배치가 엔티티만 의존성 추가해서 같이 사용할 수 있게)
- CQRS 패턴구조
- CUD는 Postgres, R은 Elastic search
- versioning도 고려한 패키지 구조
- 프로젝트 다시 짤라고 이전 코드 지웠는데 참고하려면 git log로 이전 버전까지 프로젝트 구조 어떻게 됐는지 참고정도는 가능

---

# 개요에 대한 의견

> plan2 확인

## 답변

현재 프로젝트는 단일 모듈이고 src/main에는 ChoimoryDevMemberApiApplication.java 하나만 남아 있음. 의존성에는 JPA, Elasticsearch, Redis, Postgres, Testcontainers, spotless가 이미 들어 있음. 개요의 4가지 방향에 대한 의견은 아래와 같음.

## DB 종류가 Postgres로 확정됨

plan1에서 미정이던 DB 종류가 해결되었고 아래가 달라짐.

- DB2는 id를 varchar로 적었으나 Postgres에는 uuid 타입이 있음. 저장 크기와 인덱스 효율이 더 좋으므로 uuid 타입을 권장
- "DB enum"으로 정한 컬럼은 `CREATE TYPE ... AS ENUM`으로 생성하고, 값 추가는 `ALTER TYPE ... ADD VALUE`로 처리
- MySQL 기준으로 논의한 생성 컬럼 유니크는 해당 사항 없음

## 1. Gradle 멀티 모듈 (배치가 엔티티만 의존)

방향에 동의. 아래 구성이 자연스러움.

- member-core(또는 member-domain): 엔티티, enum, 레포지토리. 배치와 API가 공통으로 의존
- member-api: 컨트롤러, 서비스, 보안 설정
- (향후) member-batch: 스프링 배치. member-core에만 의존

정해야 할 점은 다음과 같음.

- 엔티티 모듈에 레포지토리까지 둘지, 엔티티만 둘지. 배치가 DB를 직접 다룬다면 레포지토리도 같이 있는 편이 편함
- CQRS이므로 ES 문서(Document)를 어느 모듈에 둘지. 배치도 ES를 갱신해야 하면 공통 모듈 필요
- 엔티티 모듈은 JPA에만 의존하고 Spring Web, Security는 끌어오지 않게 해야 함

## 2. CQRS, 쓰기는 Postgres, 읽기는 Elasticsearch

설계의 가장 큰 결정 사항. 아래는 반드시 정해야 함.

- 동기화 방식: Postgres에 쓴 내용을 ES에 어떻게 반영할지가 핵심. 이중 쓰기는 한쪽이 실패하면 불일치가 생김. 안전한 선택지는 outbox 테이블에 이벤트를 같이 저장하고 별도 프로세스가 ES에 반영하는 방식. 처음에는 스프링 이벤트와 재시도로 시작해도 됨
- 최종 일관성: 가입 직후나 프로필 수정 직후에 읽으면 ES에 아직 반영되지 않았을 수 있음. 어떤 조회까지 이를 허용할지 결정 필요
- ES로 읽을 대상: 모든 조회를 ES로 보낼지, 검색과 목록 위주만 보낼지 결정 필요. 로그인(인증)과 이메일 중복 확인처럼 정합성이 중요한 조회는 Postgres에서 하는 것을 강하게 권장. 인증이 ES 장애에 영향받으면 안 됨
- 삭제 동기화: soft delete(탈퇴)와 hard delete 배치가 ES 문서에도 반영되어야 함. 탈퇴철회를 지원하므로 탈퇴 시 ES 문서를 지울지 숨김 처리할지 결정 필요
- 범위: 회원 서비스 데이터는 규모가 크지 않은 편이라 모든 도메인을 CQRS로 나누면 과할 수 있음. 회원 목록 검색이나 닉네임 검색처럼 ES의 이점이 있는 조회부터 적용하는 방법도 있음 (이미 결정된 방향이므로 참고만)

## 3. 버전을 고려한 패키지 구조

"versioning"이 무엇의 버전인지부터 정해야 함.

- API 버전(/v1, /v2)이라면 컨트롤러와 요청/응답 DTO 계층만 버전별로 나누고 엔티티와 도메인 서비스는 공유하는 구조를 권장. 예: api.v1.member.controller, api.v1.member.dto
- ES 인덱스 버전이라면 인덱스 이름에 버전을 붙이고 별칭(alias)으로 읽는 방식이 재색인에 안전
- 둘 다라면 각각 따로 정리

패키지 분리 기준(계층별인지 기능별인지)도 함께 정해야 함. 기능별(member, follow, agreement…)로 나누고 그 안에서 계층을 나누는 방식을 권장. 이전 코드의 구조는 git log로 참고 가능.

## 다음에 정하면 좋을 순서

1. "versioning"의 의미(API 버전인지 ES 인덱스 버전인지)
2. 모듈 구성과 각 모듈의 책임
3. CQRS 동기화 방식과 ES로 읽을 대상 범위
4. 패키지 구조와 엔티티 설계

---

# 추가1

1. versioning은 api 버전
2. 모듈은 도메인과 api로 나눌거고, 도메인은 entity, document 같은 JPA, ES 내용만 담을거임. 요청 응답객체와 DTO는 어디다 넣어야할지 고민이지만 지금 생각은 DTO는 도메인, 요청 응답객체는 API쪽이 맞는거 같음
3. postgres to es는 내가 나중에 메시지큐 기반 컨슈머 만들거임. 나중에 kafka에 던지기만 하면 됨. 이건 지금 당장 개발할거 아니고 로직중 해당 단계에서 TODO 주석으로 flow에 화이트보딩만 해놓으면 됨
4. postgres에만 작성되고 es에 아직 반영안됐으면 그냥 안보여도 됨. 정책상 그정도의 내용은 허용할거임
5. es로 읽을 대상은 성능이나 정합성의 중요도를 고려해서 api별로 결정할거임. 조회 api중에 postgres를 쓰는게 필요하면 그 api는 postgres로 조회할거임
6. 규모 상관없이 일단 진행할거임

--

# 추가1에 대한 의견

> 79번줄 확인

## 답변

79번줄은 `# 추가1`이며, 6개 항목에 대한 의견은 아래와 같음.

## 1. versioning은 API 버전

확정. 요청과 응답 객체, 컨트롤러만 버전별로 나눔. 예: API 모듈의 v1.member.controller, v1.member.request, v1.member.response. 도메인 모듈은 버전 없이 공유.

## 2. 모듈은 domain과 api, DTO는 도메인에, 요청/응답 객체는 API에

모듈 분리에 동의하고 DTO 위치 판단도 방향은 맞음. API 버전이 올라가도 도메인 DTO는 그대로 두고 버전별 응답 객체로만 변환하면 되므로 버전 분리와 잘 맞음. 정해야 할 점은 다음과 같음.

- DTO가 무엇인지 먼저 정해야 함. 서비스가 반환하는 결과 객체, 조회 프로젝션, 서비스 입력(command)이라면 도메인에 두는 것이 맞음. 엔티티를 그대로 옮기는 단순 복사본이라면 계층만 늘어남. 엔티티, DTO, 응답 객체 3종 변환이 모든 API에서 필요한지 확인하고, 필요한 곳에만 DTO를 두는 것을 권장
- 도메인 DTO는 API 모듈이나 웹 관련 코드를 참조하면 안 됨. 검증 어노테이션(@Valid 등)은 요청 객체에 둠
- 레포지토리와 서비스의 위치 결정 필요. "도메인은 entity, document만"이라고 했으나 레포지토리(JPA, ES)를 어디에 둘지 미정. 도메인 모듈에 두면 향후 배치가 같은 레포지토리를 그대로 쓸 수 있어 권장. 서비스는 API 모듈에 두고 배치 쪽 로직은 배치 모듈에서 따로 만드는 것으로 이해함
- 도메인 모듈에 JPA와 ES를 같이 두면 배치가 JPA만 필요해도 ES 의존성이 따라옴. 지금은 문제 없고, 필요해지면 그때 분리 가능

## 3. Postgres에서 ES로의 동기화는 나중에 Kafka 컨슈머, 지금은 TODO 주석

방향에 동의. 지금 개발하지 않는 부분을 흐름상의 위치만 표시해 두는 방식은 합리적임. 권장 사항은 다음과 같음.

- TODO 주석은 `// TODO(kafka): ...`처럼 일정한 태그를 붙여 두면 나중에 검색해서 위치를 찾기 쉬움
- 발행 지점은 DB 트랜잭션이 커밋된 뒤여야 함. 커밋 전에 발행하면 롤백된 변경이 ES에 반영될 수 있음. 주석에 "커밋 후 발행"을 적어 두면 구현 시 놓치지 않음
- DB는 커밋됐는데 Kafka 발행이 실패하면 불일치가 생김. 나중에 구현할 때 outbox 같은 방식으로 다룰 문제이므로 지금은 주석에만 적어 두면 충분

## 4. ES에 반영되기 전에는 안 보여도 허용

정책으로 확정한 것에 동의. 실무적으로 컨슈머가 만들어지기 전에는 ES에 데이터가 아예 없음. 그동안 ES를 읽는 API는 항상 빈 결과를 줌. 컨슈머 전까지는 ES 조회 API를 만들지 않거나 Postgres로 임시 조회하게 할지, 테스트에서 ES에 직접 데이터를 넣을지 정해 두는 것이 좋음.

## 5. ES로 읽을 대상은 API별로 결정

동의. API마다 읽는 저장소를 정하고 기준(성능, 정합성)과 함께 문서에 표로 남겨 두는 것을 권장. 로그인과 이메일 중복 확인처럼 정합성이 중요한 조회는 Postgres로 가는 것을 다시 권장.

## 6. 규모와 상관없이 진행

이 결정을 기준으로 진행.

## 정리

- 확정: 1, 3, 4, 5, 6
- 확인 필요: 2번의 DTO 정의, 레포지토리와 서비스의 위치
- 앞서 정한 순서에서 남은 것은 패키지 구조와 엔티티 설계

---

# 추가2

- 데이터객체는 dto, api 요청객체, api 응답객체로 나뉨. 이 중 dto는 엔티티 객체의 1:1 역할을 함 (백엔드 코드 컨벤션 문서 참고)
- api 요청 응답객체는 api에서만 쓰니까 도메인에 없어도 될거 같고, 엔티티랑 dto는 도메인쪽에 넣어야 하지 않을지
- 엔티티 외에도 es의 document 같은것도 도메인에 넣어야 할거 같고
- handler, service도 api에서만 쓰니까 api에 넣고, repo는 흠... 배치도 쓰니까 domain에 넣어야하나? 그래도 api에 넣는게 분류가 일관될거 같음. domain = 데이터 객체로 보면 되니까
- es 조회 api는 추후 api 구현시 만들거고 지금은 안만들거임

--

# 추가2에 대한 의견

> 141번줄 확인

## 답변

141번줄은 `# 추가2`이며, 5개 항목에 대한 의견은 아래와 같음. 백엔드 코드 컨벤션 문서(.agents/rules/back-end/code-convention-springboot.md)도 함께 확인함.

## 전반

컨벤션 기준으로 의존 방향이 일관됨. DTO.from(Entity)는 DTO가 엔티티를 참조하고(둘 다 domain 안), Request.toEntity()와 Response.from(DTO)는 API가 domain을 참조함. 즉 api → domain 한 방향만 생기므로 모듈 분리에 문제가 없음.

## 1. 데이터 객체는 dto, 요청 객체, 응답 객체로 나뉘고 dto는 엔티티와 1:1

확정. 추가1에서 확인이 필요하던 "DTO가 무엇인가"는 컨벤션 문서로 해결됨.

## 2. 요청/응답 객체는 API, 엔티티와 DTO는 도메인

동의. 컨벤션상 Request.toEntity()가 엔티티를 생성하는데 API가 domain을 의존하므로 방향도 맞음. API 버전별로 요청/응답 객체만 나누고 DTO는 그대로 쓰는 구조도 이 배치와 잘 맞음.

## 3. ES Document도 도메인

동의. 확인 필요 사항은 Document마다 DTO를 따로 만들지, 엔티티의 DTO를 그대로 재사용할지. 컨벤션은 일부 필드만 조회할 때 나머지를 null로 두는 것을 허용하므로 Document도 같은 DTO로 변환하는 편이 나음. 그러면 API별로 Postgres와 ES를 바꿔도 응답 객체 변환은 그대로 사용 가능.

## 4. handler, service는 API에, repo는?

handler와 service는 API에 두는 것이 맞음. 컨벤션상 Service가 @Transactional과 오케스트레이션을 맡고 그 안에서 Handler를 호출함.

repo는 `domain = 데이터 객체`로 분류를 일관되게 하자는 생각이 이해되나, domain에 두는 쪽을 권장.

- 멀티 모듈의 목적이 배치의 엔티티 재사용인데, 배치가 하는 일(hard delete, 데이터 정리)은 결국 레포지토리를 통해 DB를 다루는 것. 레포지토리가 API에 있으면 배치가 같은 엔티티에 대한 레포지토리를 다시 만들어야 해서 두 군데에 생김
- 레포지토리는 JpaRepository<Entity, ID>처럼 엔티티에 직접 묶여 있고, 커스텀 레포지토리는 DTO를 반환하므로 둘 다 domain에 있어야 함
- 분류 기준을 "domain = 영속성 계층(entity, dto, document, repository)"으로 바꾸면 일관성도 유지됨

반대로 "지금은 배치가 없으니 API에 두고 배치를 만들 때 옮긴다"는 선택도 가능. 다만 그때 이동하는 파일이 많아지고 스캔 설정도 바꿔야 함. 어느 쪽이든 결정은 사용자 몫.

## 5. ES 조회 API는 나중에

확정. 추가1에서 우려한 "컨슈머 전에는 ES가 비어 있다"는 문제도 이렇게 해결됨. Document 클래스와 ES 레포지토리를 지금 만들지, ES 조회 API와 함께 나중에 만들지는 결정 필요.

TODO(kafka) 주석의 위치는 컨벤션상 Service가 @Transactional을 맡으므로 Service 함수에서 Handler 호출이 끝난 뒤 지점이 자연스러움. 커밋 후 발행이어야 한다는 점도 주석에 같이 적을 것.

## 곧 부딪힐 것: ID 생성

컨벤션 예시는 Long id + IDENTITY인데 이 프로젝트는 UUID v7을 사용함. 엔티티 설계에서 정해야 함. build.gradle에는 uuid-creator가 있고, Spring Boot 3.5의 Hibernate는 @UuidGenerator로 v7 생성도 지원. 공통 컬럼(created_at 등)과 soft delete를 다룰 베이스 엔티티도 함께 정할 항목.

## 정리

- 확정: 1, 2, 3(DTO 재사용 방향), 5
- 확인 필요: repo를 domain에 둘지 API에 둘지, Document 클래스를 지금 만들지
- 위 두 가지가 정해지면 패키지 구조 초안을 제안  

---

# 추가3

1. repo는 api
2. dto는 entity의 dto, document의 dto 별도로 구분하여 각각 생성 (es의 인덱스가 db의 테이블과 동일하게 구성할거라는 보장이 없음)
3. 컨벤션은 Long id지만 여기서는 uuid varchar를 사용할거임. postgres의 uuid는 v7 지원하는지 모르겠음. 어쨌든 그런 저런 이유로 비즈니스 로직을 이용해 uuid를 채번해서 id varchar에 넣는식으로 비즈니스로직에서 녹일거임

--

# 추가3에 대한 의견

> 207번줄 확인

## 답변

207번줄은 `# 추가3`이며, 3개 항목에 대한 의견은 아래와 같음.

## 1. repo는 api

확정. 나중에 배치를 만들 때 배치용 레포지토리를 따로 만들거나 그때 domain으로 옮기게 됨. 지금은 문제 없는 선택. 실무적으로 엔티티가 domain 모듈에 있고 레포지토리가 API 모듈에 있으므로, API 애플리케이션에서 엔티티 스캔과 레포지토리 스캔 패키지를 명시해야 함(@EntityScan, @EnableJpaRepositories). 패키지 구조를 정할 때 같이 챙길 것.

## 2. entity의 DTO와 document의 DTO를 별도로 생성

이유에 동의. ES 인덱스는 검색용으로 비정규화하거나 필드가 달라질 수 있어 테이블과 같다는 보장이 없음. 따라서 추가2에서 제안한 "DTO 재사용"은 철회. 이 결정의 결과는 다음과 같음.

- 응답 객체가 두 DTO에서 모두 만들어질 수 있어야 하므로 Response.from(MemberDto)와 Response.from(MemberDocumentDto)처럼 정적 팩토리 메소드가 둘이 됨. API가 저장소를 바꿔도 응답 객체 자체는 그대로 사용 가능
- 컨벤션 문서는 "Entity와 DTO 1:1"만 적고 있으므로 Document와 Document DTO의 1:1 규칙을 컨벤션에 추가하는 것도 고려 (요청할 때만 수정)
- 이름은 엔티티 DTO와 구분되게 MemberDto와 MemberDocumentDto처럼 정하는 것을 권장

## 3. UUID는 varchar로, 비즈니스 로직에서 채번

Postgres의 uuid 타입에 대한 확인 사항은 다음과 같음.

- uuid 타입은 128비트 값을 저장하는 타입이라 버전과 상관없이 v7 값도 저장 가능. 애플리케이션이 v7을 만들어 넣으면 어떤 Postgres 버전에서도 동작
- DB가 직접 v7을 생성하는 uuidv7() 함수는 Postgres 18부터 내장. 비즈니스 로직에서 채번하면 필요 없음

따라서 "v7 지원 여부"는 선택에 영향을 주지 않음. varchar로 가면 저장 공간과 인덱스가 uuid보다 커지지만(36바이트 대 16바이트) 크게 문제될 정도는 아니므로 확정해도 됨. 나중에 바꾸기도 어렵지 않음.

비즈니스 로직 채번에서 주의할 점은 다음과 같음.

- 채번 위치: 엔티티의 정적 팩토리(create)나 베이스 엔티티 한 곳에서만 만들도록 모으는 것이 좋음. 프로젝트에 이미 있는 uuid-creator에서는 `UuidCreator.getTimeOrderedEpoch()`가 v7
- save() 추가 SELECT: 애플리케이션이 ID를 직접 넣은 엔티티에 Spring Data JPA의 save()를 호출하면 새 엔티티인지 판단하지 못해 merge를 하고, INSERT 전에 SELECT를 한 번 더 실행함. 이를 막으려면 Persistable을 구현해 isNew()를 직접 정하는 방식이 일반적이며, 베이스 엔티티에서 한 번에 처리

## 정리

- 확정: 1, 2, 3
- 답이 없는 것: ES Document 클래스를 지금 만들지, ES 조회 API와 함께 나중에 만들지. 2번 결정으로 Document와 Document DTO가 각각 필요하다는 것은 확인됨. 시점만 결정 필요
- 이제 패키지 구조 초안 제안 가능

---

# 추가4

- es document 클래스는 api만들때 만들거임

--

# 추가4에 대한 의견

> es document 클래스는 api만들때 만들거임

## 답변

Document 클래스는 ES 조회 API를 만들 때 함께 만드는 것으로 확정. 지금 domain 모듈에는 엔티티와 DTO, enum만 있으면 됨. 이 결정을 바탕으로 패키지 구조 초안을 제안함.

## 모듈 구성

```
choimory-dev-member-api (루트, 코드 없음)
├ settings.gradle      include 'member-domain', 'member-api'
├ member-domain        엔티티, DTO, enum (향후 Document도)
└ member-api           controller, service, handler, repository, request, response
```

- 루트 프로젝트 이름은 그대로 두고 기존 src는 member-api로 이동
- 빌드는 member-domain이 JPA와 UUID 라이브러리에만 의존하게 하고, bootJar는 member-api에서만 활성화. spotless는 루트에서 두 모듈에 적용

## domain 패키지

기능별로 나누고 그 안에서 계층을 나눔.

```
dev.choimory.member.domain
├ common            BaseEntity (id 채번, 공통 컬럼, soft delete, Persistable)
├ member            entity: Member, MemberImage, MemberSuspension
│                   dto:    MemberDto, MemberImageDto, MemberSuspensionDto
│                   type:   MemberStatus, MemberImageType
├ follow            Follow, FollowDto
├ agreement         Agreement, AgreementAnswer, MemberAgreement + DTO
└ authority         Authority, Permission, PermissionAction, Menu, MemberAuthority + DTO
```

## api 패키지

컨벤션상 Service가 요청 객체를 받고 응답 객체를 반환하므로 Service도 버전에 묶임. 반면 Handler(DTO 반환)와 Repository는 버전과 무관하게 공유 가능. 그래서 아래처럼 나누는 구조를 권장.

```
dev.choimory.member.api
├ common            config, exception, 공통 응답
├ member            handler, repository   (버전 무관, v1과 v2가 공유)
│  follow, agreement, authority ... 같은 구조
└ v1
   └ member         controller, service, request, response
```

v2가 생기면 v2.member만 새로 만들고, 바뀌지 않은 Service는 v2가 그대로 재사용하거나 복사해서 수정 가능.

대안은 v1.member 안에 handler와 repository까지 전부 두는 것. 구조는 단순하지만 v2가 생길 때 handler와 repository가 중복됨.

## 스캔 설정

member-api의 애플리케이션 클래스가 dev.choimory.member.api 아래에 있으므로, dev.choimory.member.domain의 엔티티와 레포지토리를 읽으려면 @EntityScan과 @EnableJpaRepositories로 패키지를 명시해야 함.

## 확인이 필요한 점

1. follow를 member 패키지에 포함할지, 별도 기능 패키지로 둘지. 위 초안은 follow를 따로 둠
2. api 쪽 버전 구조를 위처럼 나눌지(권장), v1 안에 전부 둘지
3. 모듈 이름을 member-domain, member-api로 할지

---

# 추가 5

1. follow는 member 패키지
2. v1>domain 말고 domain>v1
3. 모듈이름은 그렇게 해줘
4. 패키지 구성
   - domain
     - {domain-name}/v1/command, query/entity, document, dto (entity는 command, document는 query, dto는 양쪽 모두)
   - api
     - {domain-name}, config, common/v1/command, query/controller, service, handler, repository, domain
       - {domain-name}/v1/domain/request, response, query(customquery결과 객체)
5. 이름
   - entity는 테이블명 그대로
   - entitydto는 entity명+dto
   - document는 테이블명+document
   - documentdto는 document명+dto

--

# 추가 5에 대한 의견

> 331번줄 확인

## 답변

331번줄은 `# 추가 5`이며, 5개 항목에 대한 의견은 아래와 같음. 먼저 이해한 구조를 정리하고 이어서 확인이 필요한 점을 정리함.

## 이해한 구조

```
member-domain  dev.choimory.member.domain
└ member/v1                        (agreement, authority도 같은 형태)
   ├ command/entity   Member, MemberImage, MemberSuspension, MemberAgreement, Follow ...
   ├ command/dto      MemberDto ...
   ├ query/document   MemberDocument ... (ES 조회 API 만들 때)
   └ query/dto        MemberDocumentDto ... (같은 시점)

member-api  dev.choimory.member.api
├ config
├ common
└ member/v1
   ├ command/ controller, service, handler, repository
   ├ query/   controller, service, handler, repository
   └ domain/  request, response, query(커스텀 쿼리 결과 객체)
```

- entity는 command, document는 query, dto는 양쪽에 두는 구조로 CQRS를 패키지에 반영한 것으로 이해
- 이름은 Member(엔티티), MemberDto, MemberDocument, MemberDocumentDto로 정리됨. 추가3에서 정한 "entity의 DTO와 document의 DTO를 따로" 결정과 일치

## 1, 2, 3

- 1번(follow는 member 패키지)과 3번(모듈 이름 member-domain, member-api)은 확정
- 2번의 `{domain-name}/v1` 방식(기능 아래에 버전을 두는 구조)으로 이해함. 추가4의 초안은 v1/member(버전이 먼저)였음

## 확인이 필요한 점

1. domain 모듈의 v1 의미: 추가1에서 versioning은 API 버전이고 도메인은 버전 없이 공유하는 것으로 정했는데, 현재 구조는 domain 모듈에도 v1이 있음. API의 v1과 v2가 같은 엔티티를 공유한다면 domain의 v1은 필요 없음. "데이터 모델의 버전"을 뜻한다면 같은 테이블에 대한 Member 엔티티가 v1과 v2에 둘 다 생기게 되어 @Entity 이름이 겹치고, 같은 테이블을 가리키는 클래스가 둘이라 혼란이 생길 수 있음
2. api의 handler, repository도 v1 아래에 두는 구조: 추가4에서는 v1과 v2가 공유하는 것을 권장했으나 현재 구조는 전부 버전 아래. 선택 자체는 가능하나 v2를 만들 때 클래스를 복사하면 @Component와 레포지토리의 빈 이름이 단순 클래스명이라 빈 이름 충돌(ConflictingBeanDefinitionException)이 남. 이름을 다르게 하거나, v2에서 v1을 재사용하거나, 빈 이름 생성 규칙(FQN)을 바꾸는 방법이 있음. v2를 만들 때 정해도 됨
3. api 안의 domain 패키지 이름: 모듈 이름 member-domain과 겹쳐 혼동될 수 있음. 요청, 응답, 쿼리 결과 객체를 담는 곳이므로 model이나 data 같은 이름이 나음 (선택)
4. JPA와 ES 레포지토리 공존: 두 스타터가 같이 있으면 Spring Data가 엄격 모드로 동작하여 엔티티는 JPA 레포지토리에, @Document는 ES 레포지토리에 배정됨. 어노테이션이 있는 한 문제 없음. 이전 스캔 설정(@EntityScan과 @EnableJpaRepositories로 dev.choimory.member.domain 명시)은 그대로 필요
5. BaseEntity의 위치: 구조에 공통 위치가 없음. domain 모듈 루트의 common 같은 패키지로 두는 것을 권장

## 정리

- 확정: 1, 3, 4의 전반 구조, 5(이름 규칙)
- 확인 필요: domain의 v1 의미, api 패키지 domain의 이름(선택), BaseEntity 위치

---

# 추가6

1. domain 모듈도 테이블 변경하면 api의 v1이 박살나니까 똑같이 버저닝 하려했는데 뭔가 애로사항이 많을거 같긴 하네.
2. api 패키지의 domain은 그대로 두고 domain 모듈을 model 모듈로 바꿀까? 그리고 api의 domain은 command, query와 같은 레벨 말고 하나 아래임. command, query에 각각 domain 패키지
3. baseentity는 common에 (common도 {domain}패키지처럼 동일하게 구성함)

--

# 추가6에 대한 의견

> 402번줄 확인

## 답변

402번줄은 `# 추가6`이며, 3개 항목에 대한 의견은 아래와 같음.

## 1. domain 모듈도 테이블 변경 시 api v1이 깨지니 똑같이 버저닝

걱정하는 점은 맞지만 엔티티를 버전별로 나눠도 그 문제는 해결되지 않는다고 판단함.

- DB 테이블은 하나. v1과 v2 엔티티가 같은 테이블을 가리키는 구조라서 컬럼을 삭제하거나 이름을 바꾸면 v1 엔티티도 똑같이 깨짐. 이를 막으려면 테이블 자체를 버전별로 둬야 하는데(member_v1, member_v2) 비용이 너무 큼
- 앞서 언급한 @Entity 이름 충돌과 중복 클래스 문제도 따라옴
- 이 문제를 푸는 일반적인 방법은 다음과 같음
  - 테이블 변경은 하위 호환으로 처리. 컬럼을 추가하고 이전 컬럼은 한동안 유지한 뒤 정리
  - 이전 버전의 응답 모양은 API의 응답 객체(Response.from(Dto))에서 맞춤. 현재 구조가 이미 이 역할을 하도록 되어 있음
  - 마이그레이션 도구(Flyway 등)로 스키마 변경을 관리

따라서 domain 모듈은 버전 없이 쓰는 것을 권장. "애로사항이 많을 것 같다"는 판단이 맞다고 봄.

## 2. 모듈을 model로 바꾸고, api의 domain은 command와 query 아래에 각각 둠

방향은 괜찮음. 모듈 이름과 api의 패키지 이름이 겹치는 문제를 모듈 쪽 이름을 바꿔서 푸는 방식. 정리는 다음과 같음.

- member-domain은 member-model로 바뀌고 기본 패키지도 dev.choimory.member.model이 됨. 엔티티 스캔 설정도 이 경로로 맞춰야 함
- 앞서 member-domain으로 확정한 것을 바꾸는 것이므로 이후 문서에서 이름을 통일해야 함
- api의 domain이 command와 query 각각에 생기므로 command/domain/request와 query/domain/request는 요청 객체의 성격이 다르게 분리됨. CQRS 구조와 잘 맞음
- 참고: 엔티티가 있는 모듈 이름이 model이고 요청/응답이 있는 패키지 이름이 domain이면 일반적인 용어와는 반대로 보일 수 있음. 혼란이 없다면 그대로 가도 됨

## 3. BaseEntity는 common에, common도 {domain}처럼 구성

동의. 1번 결론(버전 없음)을 따르면 common/command/entity/BaseEntity가 됨. BaseEntity는 JPA용이므로 command 쪽에만 있으면 되고, ES용 공통 클래스가 필요해지면 query 쪽에 따로 둠.

## 1번 결론을 반영한 구조

```
member-model  dev.choimory.member.model
├ common/command/entity           BaseEntity
└ member/                         (agreement, authority도 같은 형태)
   ├ command/entity, command/dto
   └ query/document, query/dto    (ES 조회 API 만들 때)

member-api  dev.choimory.member.api
├ config, common
└ member/v1
   ├ command/ controller, service, handler, repository, domain/{request, response, query}
   └ query/   controller, service, handler, repository, domain/{request, response, query}
```

api 쪽 v1은 그대로 유지. 앞서 언급한 handler와 repository의 빈 이름 충돌은 v2를 만들 때 결정.

## 정리

- 확정: 2(model 모듈), 3
- 정해야 할 것: model 모듈에서 v1을 뺄지(권장)

--

# 테이블 변경과 하위 버전 API

> 근데 궁금한건데 그럼 테이블 변경이 있는 api 수정하면 그때부턴 하위 버전의 api는 바로 못쓰게 되는거야? 보통 그러나?

## 답변

아니오. 그렇게 되면 안 되고 보통은 그렇게 두지 않음. 테이블이 바뀌어도 하위 버전 API는 계속 동작하도록 만드는 것이 API 버저닝의 목적.

## 하위 버전을 살리는 일반적인 방법

1. 하위 호환으로 스키마를 바꿈. 컬럼을 바로 지우거나 이름을 바꾸지 않음
   - 새 컬럼을 추가하고 옛 컬럼은 v1이 쓰는 동안 그대로 둠
   - 이름을 바꿀 때는 새 컬럼을 추가해 두 컬럼을 같이 채우다가 v1 종료 후 옛 컬럼을 삭제. 이 방식을 expand/contract(확장 후 축소)라고 함
2. 응답 객체가 차이를 흡수. 테이블이 바뀌어 DTO가 달라져도 v1의 Response.from(Dto)가 옛 모양으로 만들어 줌. 현재 설계에서 요청/응답 객체를 버전별로 나눈 이유가 이것
3. v1을 종료(deprecate)하는 시점에 옛 컬럼을 정리. 버전을 영구히 유지하는 것이 아니라 공지하고 일정 기간 뒤에 내림

## 자주 있는 경우와 대응

| 변경 | 대응 |
|---|---|
| 컬럼 추가 | 영향 없음. v1 응답은 새 필드를 내보내지 않으면 됨 |
| 컬럼 이름 변경 | 새 이름으로 추가하고 둘 다 채움, v1 종료 후 옛 컬럼 삭제 |
| 컬럼 삭제 | v1 종료 후에 삭제. 그 전에는 유지 |
| 컬럼 의미 변경 | v1 응답 객체에서 변환해 옛 의미로 내보냄 |
| 테이블 구조가 크게 바뀜 (분리/병합) | v1 서비스에서 새 구조를 옛 응답 모양으로 조립 |

## 실제로 어떻게 하나

- 대부분의 팀은 v1을 영원히 유지하지 않고 일정 기간 병행 운영한 뒤 종료
- 테이블이 크게 바뀌어 v1 유지가 정말 어렵다면 그 변경을 일으키는 시점에 v2를 내고 v1은 종료 일정을 공지
- 반대로 "스키마 변경이 곧 API 버전 증가"가 되도록 설계하면 버전이 너무 자주 올라가 관리가 어려워짐. 스키마는 하위 호환으로 바꾸고 API 버전은 외부 계약이 깨질 때만 올림

## 이 프로젝트에서의 의미

현재는 회원 서비스 하나를 직접 운영하는 상황이라 클라이언트도 직접 관리함. 옛 버전을 오래 유지할 필요가 없어 v1에서 v2로 한 번에 전환해도 부담이 크지 않음. 다만 모바일 앱처럼 사용자가 업데이트하지 않는 클라이언트가 생기면 병행 운영이 중요해짐.

따라서 "테이블 변경 때문에 v1이 깨진다"는 걱정은 domain 모듈을 버저닝하는 것이 아니라 스키마를 하위 호환으로 바꾸는 습관으로 풀어야 함.

--

# 스키마 하위 호환 운영 방식

> 스키마를 하위호환으로 둔다는게 한테이블에 v1의 컬럼, v2에 컬럼을 모두 만들어놓고 나중에 v1 컬럼을 제외하는식으로 운영한다는건가?

## 답변

큰 흐름은 맞음. 다만 "v1 컬럼"과 "v2 컬럼"이 API 버전별로 따로 있는 것이 아니라, 옛 컬럼과 새 컬럼이 전환 기간 동안 한 테이블에 같이 있는 것. 그리고 모든 변경에 이렇게 하는 것은 아님.

## 예시: member.nickname을 display_name으로 이름 변경

1. 확장(expand): display_name 컬럼을 NULL 허용으로 추가. 기존 nickname은 그대로 둠
2. 채우기: 기존 데이터를 display_name으로 복사(backfill). 이후 쓰기 때는 두 컬럼을 같이 채움
3. 전환: v2 API는 display_name을 읽고, v1 API는 계속 nickname을 읽음
4. 축소(contract): v1을 종료한 뒤 nickname을 삭제. display_name에 NOT NULL을 걸 수도 있음

## API 버전과 컬럼의 관계

- 컬럼은 v1이나 v2 소유가 아니라 전환 기간 동안만 공존하는 옛 컬럼과 새 컬럼
- 어떤 버전이 어느 컬럼을 읽는지는 서비스와 응답 객체에서 정함. 엔티티는 전환 기간에 두 컬럼을 모두 매핑하고, v1 응답은 옛 필드를, v2 응답은 새 필드를 채움

## 주의할 점

- 쓰기 동기화: 두 컬럼을 같이 채워야 하므로 서비스나 엔티티에서 항상 함께 써야 함. 한쪽만 쓰면 데이터가 어긋나므로 테스트로 지켜 주는 것이 좋음
- 새 컬럼은 NULL 허용으로 시작. NOT NULL로 추가하면 기존 행 때문에 실패하거나, v1이 값을 채우지 않을 때 깨짐
- 되돌릴 수 있도록 마지막 삭제 전까지 옛 컬럼을 유지하면 문제가 생겨도 롤백이 쉬움
- 단순한 변경은 공존이 필요 없음. 컬럼 추가는 그냥 추가하면 되고(옛 버전은 모르고 지나감), 값의 의미가 안 바뀌는 변경은 응답 객체에서 변환만 하면 됨. 공존이 필요한 것은 이름 변경, 타입 변경, 의미 변경, 삭제 같은 파괴적 변경뿐
- 이 프로젝트는 클라이언트를 직접 관리하므로 파괴적 변경이 생기면 v1을 종료하고 한 번에 바꾸는 선택도 현실적

--

# model 모듈 v1 제거와 model/domain 용어

> model 모듈 v1 빼는걸로 하자. 근데 아까 니가 model과 domain의 용어가 거꾸로 사용된다고 했는데 맞아? db는 모델링한다고 하고, Domain은 데이터객체 전반을 얘기하지 않아 보통?

## 답변

model 모듈에서 v1을 제거하는 것으로 확정. 용어 문제는 이전 답변이 과했음. 하나로 정해진 표준은 없음.

## 용어 정리

- DDD의 domain: 비즈니스 영역 전체, 또는 그 영역의 모델(엔티티, 값 객체, 규칙)을 뜻함. "데이터 객체 전반"이라는 쓰임과도 크게 어긋나지 않음
- DB 모델링의 model: 테이블과 관계를 설계하는 일, 즉 데이터 모델을 말함. 엔티티 클래스를 model이라고 부르는 것도 흔함
- Spring 프로젝트의 관례: domain에는 엔티티를 두고 model이나 dto에는 전달용 객체를 두는 구성이 많고, 반대 구성도 흔함. 프로젝트마다 다름

따라서 현재 구조(member-model 모듈에 엔티티, 도큐먼트, DTO를 두고 api의 domain 패키지에 요청, 응답, 쿼리 결과를 두는 것)가 틀린 것은 아니며 일관성 있게 정의해 두면 됨.

## 이름을 바꾼 계기

원래 이름을 바꾼 계기는 "api의 domain 패키지와 모듈 이름 domain이 겹친다"는 점이었음. 그 충돌은 실제로 있었고, 모듈을 model로 바꾸면 해결됨. 반대로 모듈은 member-domain으로 두고 api 패키지를 model(또는 payload)로 바꿔도 똑같이 해결됨. 어느 쪽이든 가능. "domain = 데이터 객체 전반"이라는 기준이라면 api의 domain을 유지하고 모듈만 model로 바꾼 현재 안이 그 정의에 더 가까움.

## 권장 사항

현재 안(member-model + api의 domain)을 유지하고 정의를 README에 한 줄씩 적어 둠.

- member-model: 영속성 데이터 객체(엔티티, 도큐먼트, 엔티티/도큐먼트 DTO)
- api의 domain: API 입출력 데이터 객체(요청, 응답, 쿼리 결과)

## 정리

- 확정: model 모듈에서 v1 제거. 이에 따라 이 문서에 정리한 구조(member-model/member/command/entity 등)가 최종 형태
- 이름은 현재 안(member-model + api의 domain)으로 유지하는 것으로 이해함. 확인 필요

--

# 최종 모듈명과 패키지 구조

> member-domain, api.payload로 하자

## 답변

이 섹션의 이름과 구조가 최종. 앞선 섹션에서 member-model, api의 domain 패키지라고 적힌 부분은 이 섹션으로 대체됨. 기존 글은 수정하지 않고 경위를 남기기 위해 그대로 둠.

## 최종 구조

```
member-domain  dev.choimory.member.domain          (v1 없음)
├ common/command/entity           BaseEntity
└ member/                         (agreement, authority도 같은 형태, follow는 member에 포함)
   ├ command/entity, command/dto
   └ query/document, query/dto    (ES 조회 API 만들 때)

member-api  dev.choimory.member.api
├ config, common
└ member/v1
   ├ command/ controller, service, handler, repository, payload/{request, response, query}
   └ query/   controller, service, handler, repository, payload/{request, response, query}
```

## 변경 경위

- 모듈은 member-domain, member-api. 추가5에서 확정했던 이름으로 돌아옴 (추가6에서 member-model로 변경을 검토했으나 최종적으로 member-domain 유지)
- api의 domain 패키지는 payload로 변경. 모듈 이름과 겹치던 문제가 해소됨
- model/domain 용어는 표준이 하나로 정해져 있지 않으므로 일관되게 정의하는 것이 중요 (이전 섹션 참고)

## 이름 규칙

- entity는 테이블명 그대로: Member
- entity dto는 entity명+Dto: MemberDto
- document는 테이블명+Document: MemberDocument
- document dto는 document명+Dto: MemberDocumentDto

## 스캔 설정

엔티티 스캔은 dev.choimory.member.domain을 명시(@EntityScan, @EnableJpaRepositories).

## 정의 (README 반영 예정)

- member-domain: 영속성 데이터 객체(엔티티, 도큐먼트, 엔티티/도큐먼트 DTO)
- api의 payload: API 입출력 데이터 객체(요청, 응답, 쿼리 결과)

README.md 반영은 작업 마무리 단계에서 진행.

## 다음 단계

- 엔티티 설계: BaseEntity(UUID v7 채번, 공통 컬럼, soft delete, Persistable)부터 시작
