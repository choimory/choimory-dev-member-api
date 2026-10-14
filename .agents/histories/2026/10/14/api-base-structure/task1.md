# 목차

- [개요](#개요)
- [API 기본 구조 클래스 생성](#api-기본-구조-클래스-생성)
- [요청 주소 적용](#요청-주소-적용)

---

# 개요

- plan1에서 확정한 API 기본 구조를 `member-api` 모듈에 구현한다.
- 4개 도메인(member, authority, agreement, menu)의 Controller, Service, Handler, Repository를 클래스와 필드까지만 작성한다.
- 메소드, 공통 응답, 예외 처리는 작업 범위에서 제외한다. (공통 응답과 예외 처리는 이후 작업)
- 규칙은 `.agents/rules/back-end`의 코드 컨벤션과 주석 규칙을 따른다.

---

# API 기본 구조 클래스 생성

> 작업 시작해줘

## 답변

- calendar.md를 `PLAN`에서 `TASK`로 변경하고 작업을 시작했다.

## 생성 내용

- 경로: `member-api/src/main/java/dev/choimory/member/api/{domain}/v1/{command|query}/{controller|service|handler|repository}`
- 클래스 28개: Controller 8, Service 8, Handler 8, Repository 4
- query repository 패키지 4곳에는 `.gitkeep`만 두었다.

| 구분 | 클래스 | 어노테이션 | 주입 필드 |
|---|---|---|---|
| Controller | `{Domain}{Command|Query}Controller` | `@RestController`, `@RequiredArgsConstructor` | `{Domain}{Command|Query}Service` |
| Service | `{Domain}{Command|Query}Service` | `@Service`, `@RequiredArgsConstructor` | `{Domain}{Command|Query}Handler` |
| command Handler | `{Domain}CommandHandler` | `@Component`, `@RequiredArgsConstructor` | `{Domain}Repository` |
| query Handler | `{Domain}QueryHandler` | `@Component` | 없음 (query repository가 비어 있음) |
| Repository | `{Domain}Repository` | 없음 (인터페이스) | `JpaRepository<{Domain}, String>` 상속 |

- `Menu` 엔티티는 도메인 모듈의 `authority.command.entity` 패키지에 있으므로 해당 패키지에서 import 한다.
- 클래스에는 용도를 설명하는 Doc Comment를, 필드에는 필드 옆에 간략한 주석을 작성했다.

## 검증

- `spotlessApply`로 포맷을 적용하고 `:member-api:compileJava`가 오류 없이 통과하는 것을 확인했다.
- 테스트 코드는 작성하지 않았고 실행하지도 않았다. (메소드가 없어 단위 테스트 대상이 없다)

---

# 요청 주소 적용

> 요청주소는 /v1/{domain}

## 답변

- Controller 8개에 `@RequestMapping("/v1/{domain}")`을 적용했다.
- command와 query Controller는 같은 기본 주소를 사용한다. 메소드 작성 시 HTTP 메소드와 하위 경로로 구분한다.

| 도메인 | 기본 주소 |
|---|---|
| member | `/v1/member` |
| authority | `/v1/authority` |
| agreement | `/v1/agreement` |
| menu | `/v1/menu` |

## 검증

- `spotlessApply`와 `:member-api:compileJava`를 다시 실행해 오류가 없는 것을 확인했다.
- grep으로 Controller 8개 모두에 주소가 적용된 것을 확인했다.

---
