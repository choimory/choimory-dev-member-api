# 목차

- [개요](#개요)
- [패키지 구조, 도메인 범위, repository 대상, 필드 범위](#패키지-구조-도메인-범위-repository-대상-필드-범위)
- [query repository와 도메인 분리](#query-repository와-도메인-분리)
- [Handler 구성과 repository 패키지](#handler-구성과-repository-패키지)
- [클래스 네이밍 확정](#클래스-네이밍-확정)
- [요청 주소](#요청-주소)

---

# 개요

- 도메인별 패키지 만들고 controller, service, handler, repository 생성
- 클래스, 필드까지만 작성하고 메소드는 아직 X

---

# 패키지 구조, 도메인 범위, repository 대상, 필드 범위

> 1. 패키지구조는 domain/v1/command, query/controller, service, handler, repository
>
> 2. 도메인범위는 세 도메인 모두
>
> 3. repository는 루트엔티티만 만들고 필요하면 추후 추가
>
> 4. .agents/rules 문서 참고
>
> 5. 필드는 final 생성자 주입, 내가 말한 필드범위는 지금 작업할때 필드까지는 작성하라는 얘기
>
> 6. 공통 응답, 예외는 이거 다음에 step2

## 답변

- 패키지는 `dev.choimory.member.api.{domain}.v1.{command|query}.{controller|service|handler|repository}` 형식으로 구성한다.
- 도메인은 member, authority, agreement 세 도메인을 모두 대상으로 한다.
- repository는 루트 엔티티(Member, Authority, Agreement)만 만들고, 나머지는 필요할 때 추가한다.
- 필드는 `@RequiredArgsConstructor`와 `private final` 필드로 생성자 주입만 작성하고, 메소드는 작성하지 않는다.
- 공통 응답과 예외 처리는 이번 범위에서 제외하고 step2에서 진행한다.

## 적용 규칙 (.agents/rules/back-end)

### 레이어 호출 규칙

- Controller는 Service만 주입받고, Controller 함수는 하나의 Service 함수만 호출한다.
- Service는 Handler를 주입받고, 다른 도메인의 Handler도 호출할 수 있다. `@Transactional`은 Service 함수가 담당한다.
- Handler는 Service에서만 호출하며, 자기 도메인의 Repository만 주입받는다.
- Handler 함수 하나는 하나의 세부 로직만 담당한다. (메소드 단위의 책임)
- Repository는 데이터 접근 책임만 가진다.

### 주석 규칙

- 클래스에는 용도를 설명하는 Doc Comment를 작성한다.
- 필드에는 필드 옆에 필드명을 설명하는 간략한 주석을 작성한다.
- 메소드는 이번 작업 범위에 없으므로 메소드 주석은 해당하지 않는다.

### 기타

- 엔티티 id가 `String`(UUID)이므로 Repository는 `JpaRepository<Member, String>` 형태로 작성한다. (규칙 문서 예시의 `Long`과 다르지만 프로젝트 기준을 따른다)

---

# query repository와 도메인 분리

> query는 조회라서 보통 es 쿼리가 될거임 일단 비워둬 menu도 루트고 별도 도메인 만들어 query도 service와 handler는 당연히 만들어

## 답변

- query는 조회 용도라 ES 쿼리가 될 예정이므로, query repository는 이번 작업에서 비워 둔다.
- Menu도 루트 엔티티로 보고 repository를 만들며, api에서는 별도 `menu` 도메인으로 분리한다.
  - 도메인 모듈에서는 `Menu` 엔티티가 authority 패키지에 있지만, api 모듈에서는 별도 도메인으로 둔다.
- query에도 Service와 Handler를 만든다.
- 이에 따라 도메인은 member, authority, agreement, menu 4개가 된다.

| 도메인 | command repository | query repository |
|---|---|---|
| member | `MemberRepository` | 비워 둠 |
| authority | `AuthorityRepository` | 비워 둠 |
| agreement | `AgreementRepository` | 비워 둠 |
| menu | `MenuRepository` | 비워 둠 |

- authority에서 메뉴 정보가 필요하면 Service에서 menu Handler를 호출하는 구조로 간다. (Handler는 자기 도메인 Repository만 호출하므로) 이번에는 필드와 클래스만 만들기 때문에 주입은 걸지 않는다.

---

# Handler 구성과 repository 패키지

> handler는 도메인명으로 할거야 . queryrepository는 .gitkeep하나 넣어놔

## 답변

- Handler는 도메인 기준으로 만든다. 클래스는 command와 query에 도메인당 하나씩 두고, 세부 로직은 추후 메소드 단위로 나눈다.
- command와 query가 같은 이름(`MemberHandler`)을 쓰면 Spring 빈 이름(`memberHandler`)이 충돌하므로, 클래스명에 Command/Query를 구분해서 붙인다.
- query repository 패키지는 클래스가 없으면 git이 빈 디렉토리를 추적하지 않으므로 `.gitkeep`만 둔다.

---

# 클래스 네이밍 확정

> MenuQueryController, MenuQueryService, MenuQueryHandler 이런식임 다

## 답변

- Controller, Service, Handler 모두 `{Domain}{Command|Query}{Layer}` 형식으로 만든다. (예: `MenuQueryController`, `MenuQueryService`, `MenuQueryHandler`)
- Repository는 `{Domain}Repository` 형식으로 만든다.

## 생성 대상

- 도메인당 Controller, Service, Handler를 command와 query에 하나씩, command Repository를 1개 만든다.
- 4개 도메인이므로 총 28개 클래스다. (Controller 8, Service 8, Handler 8, Repository 4)
- query repository 패키지에는 `.gitkeep`만 둔다.

### 패키지 구조 (member 예시)

```text
dev.choimory.member.api.member.v1
├── command
│   ├── controller/MemberCommandController
│   ├── service/MemberCommandService
│   ├── handler/MemberCommandHandler
│   └── repository/MemberRepository
└── query
    ├── controller/MemberQueryController
    ├── service/MemberQueryService
    ├── handler/MemberQueryHandler
    └── repository/.gitkeep
```

### 필드 (생성자 주입)

| 클래스 | 주입 필드 |
|---|---|
| `{Domain}CommandController` | `{Domain}CommandService` |
| `{Domain}QueryController` | `{Domain}QueryService` |
| `{Domain}CommandService` | `{Domain}CommandHandler` |
| `{Domain}QueryService` | `{Domain}QueryHandler` |
| `{Domain}CommandHandler` | `{Domain}Repository` |
| `{Domain}QueryHandler` | 없음 (query repository가 비어 있음) |

## 작업 범위

- 포함: 4개 도메인의 클래스와 필드, 클래스와 필드 주석, query repository `.gitkeep`
- 제외: 메소드, 공통 응답과 예외 처리(step2)

---

# 요청 주소

> 요청주소는 /v1/{domain}

## 답변

- Controller의 기본 요청 주소는 `/v1/{domain}` 형식으로 한다.
- command와 query Controller는 같은 기본 주소를 사용하며, 메소드를 작성할 때 HTTP 메소드와 하위 경로로 구분한다.
- Controller 클래스에 `@RequestMapping("/v1/{domain}")`을 붙여 적용한다.

| 도메인 | 기본 주소 |
|---|---|
| member | `/v1/member` |
| authority | `/v1/authority` |
| agreement | `/v1/agreement` |
| menu | `/v1/menu` |

---
