# choimory-dev-member-api

## 개요

- Choimory 개발 회원 API 프로젝트다.
- Java 21 기반 Spring Boot 회원 서버이며 Gradle 멀티 모듈로 구성한다.
- 회원 도메인의 DB 설계(DDL)와 엔티티, DTO, enum, 공통 응답과 예외 핸들러, Flyway 마이그레이션이 구현되어 있다.
- `member-api`는 회원가입, 로그인, 토큰 재발급, 로그아웃(JWT, Redis)이 구현되어 있고, 그 외 API(`authority`, `agreement`, `menu`와 query)는 도메인별 기본 구조(클래스와 필드만, 메소드 없음)만 있다.
- 작업 히스토리는 `.agents/histories`를 참고한다. (DB 설계와 구현: `.agents/histories/2026/02/17/modeling`, API 기본 구조와 공통 응답, 예외: `.agents/histories/2026/10/14/api-base-structure`, Flyway 적용: `.agents/histories/2026/10/14/flyway-apply`, 회원가입, Spring Security, JWT, 로그인/로그아웃: `.agents/histories/2026/10/19/member-join-security-auth`)

## 기술 구성

- Language
  - Java 21
- Build
  - Gradle Groovy DSL, Gradle Wrapper
  - Palantir Java Format, Spotless
- Framework
  - Spring Boot 3.5.x, Spring Data JPA
  - Spring Web, Spring Security, Spring Validation (member-api)
  - Spring Data Redis (member-api, Refresh Token과 Access Token 블랙리스트 저장)
  - Spring Data Elasticsearch (member-api, 의존성만 있고 설정은 이후 추가)
- Library
  - Hibernate, UUID Creator, Lombok, PostgreSQL Driver
  - jjwt 0.12.x (JWT)
- Test
  - JUnit5, Spring Boot Test, Testcontainers PostgreSQL 16, Redis 7

## 프로젝트 구조

```text
.
├── build.gradle                  # 공통 플러그인, Java 21 toolchain, Spotless 설정
├── settings.gradle               # member-domain, member-api
├── docs
│   ├── api/v1                    # OpenAPI 3.1 명세서 (도메인별 JSON)
│   │   ├── choimory-dev-member-api-member-v1.json     # 회원가입, 토큰 재발급
│   │   └── choimory-dev-member-api-security-v1.json   # 로그인, 로그아웃
│   ├── db/v1                     # 참고용 DDL, DML (앱과 테스트에서 사용하지 않는다)
│   │   ├── choimory-dev-member-api-ddl-v1.sql   # 테이블 DDL (12개 테이블)
│   │   └── choimory-dev-member-api-dml-v1.sql   # 초기 데이터 (권한)
│   └── flyway/v1                 # Flyway 마이그레이션 원본
│       ├── V1__init_schema.sql                  # 테이블 DDL (12개 테이블, COMMENT는 테이블별로 포함)
│       ├── V2__timestamp_to_timestamptz.sql     # 시각 컬럼을 timestamptz로 변경
│       └── V3__init_authority_data.sql          # 권한 기본 데이터 (MEMBER, ADMIN)
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
        │   ├── config                    # JpaConfig, PasswordEncoderConfig, SecurityConfig
        │   ├── common/exception          # CommonException, CommonExceptionHandler
        │   ├── {member,authority,agreement,menu}/v1/{command,query}/{controller,service,handler,repository,payload}
        │   ├── jwt/v1/command            # handler, repository, util(JwtProvider), payload/{dto,type}
        │   └── security                  # filter, handler, payload/request, user
        └── resources
            ├── application.yml, application-datasource.yml, application-flyway.yml
            ├── application-jwt.yml, application-redis.yml
            └── application-{local,dev,prod}.yml
```

- `member-api`는 `member-domain`에 의존하며, `JpaConfig`가 엔티티는 `dev.choimory.member.domain`, 레포지토리는 `dev.choimory.member.api` 패키지에서 스캔한다.
- 빌드 시 `docs/flyway`가 `member-api`의 classpath `db/migration/` 아래로 복사된다. (`processResources`, 결과: `db/migration/v1/V1__init_schema.sql` 등)

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
- 시각 필드는 `java.time.Instant`이고 DB 컬럼은 `timestamptz`다. (`createdAt`, `modifiedAt`, `deletedAt`, 정지 기간 `suspendedAt`, `suspendedTo`) DTO도 `Instant`를 쓰며, JSON은 UTC 기준 ISO 문자열로 나간다. 달력 계산(예: 한 달 뒤)이 필요하면 `ZoneId`를 붙여 변환한다.

## DTO 규칙

- Java `record`와 `@Builder(toBuilder = true)`를 사용하고, 정적 `from(Entity)`로 변환한다. (Entity에는 DTO 변환 메소드를 두지 않는다)
- 자식 DTO에는 부모 객체와 부모 id를 두지 않는다.
- 참조만 하는 관계는 `fromWithoutChildren`으로 변환해 하위 컬렉션을 건드리지 않는다.
- 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 변환한다.

## API 구조

- 도메인은 `member`, `authority`, `agreement`, `menu`, `jwt`이며, 패키지는 `dev.choimory.member.api.{domain}.v1.{command|query}.{controller|service|handler|repository|payload}`다. `jwt`는 토큰 처리만 하는 하위 모듈이라 controller와 service 없이 handler, repository, util, payload만 있고, 로그인·로그아웃은 `security` 패키지의 필터가 처리한다.
- 클래스 이름은 Controller, Service, Handler가 `{Domain}{Command|Query}{Layer}`(예: `MenuQueryController`), Repository가 `{Domain}Repository`다. command와 query의 Handler 이름이 같으면 Spring 빈 이름이 충돌하므로 Command, Query를 구분해 붙인다.
- 요청 주소는 Controller 클래스의 `@RequestMapping("/v1/{domain}")`이며, command와 query Controller가 같은 기본 주소를 쓴다. 로그인(`/v1/login`)과 로그아웃(`/v1/logout`)은 컨트롤러가 아니라 Security 필터가 처리하므로 이 규칙의 예외다.
- 주입 관계는 Controller → Service → Handler → Repository이며 `@RequiredArgsConstructor`와 `private final` 필드로 생성자 주입한다. Handler는 자기 도메인의 Repository만 주입받고, Service는 다른 도메인의 Handler를 호출할 수 있다. (예: `MemberCommandService`가 `AuthorityCommandHandler`, `JwtCommandHandler` 호출) Security 필터는 컨트롤러·서비스 계층 밖이라 필요하면 핸들러를 직접 호출한다.
- `payload` 패키지는 요청·응답(`payload/request`, `payload/response`)뿐 아니라 해당 도메인의 데이터 객체(`payload/dto`)와 enum(`payload/type`)을 모두 담는다. (엔티티와 1:1인 DTO는 `member-domain`의 `command/dto`)
- command Repository는 루트 엔티티(Member, Authority, Agreement, Menu)마다 `JpaRepository<엔티티, String>`으로 두고, 필요하면 추가한다.
- query Repository 패키지는 비어 있으며(`.gitkeep`) Elasticsearch 조회로 구현할 예정이다. query Handler는 Repository 없이 클래스만 있다.
- 구현된 API는 아래 "인증과 보안"의 표를 참고한다. 그 외 도메인은 클래스와 필드만 있고 메소드는 없다.

### 회원가입 규칙

- `POST /v1/member/join`: 이메일, 비밀번호, 닉네임, 자기소개를 받아 회원을 `ACTIVATE` 상태로 저장하고 기본 권한 `MEMBER`를 부여한다. 약관 검증·동의 이력 저장과 이메일 인증 queue 전송은 TODO다.
- 입력 검증: 이메일 형식(도메인에 점이 있어야 한다), 비밀번호 8~64자에 영문·숫자·특수문자 포함, 닉네임 한글·영문·숫자만 3~15자, 자기소개 500자 이하
- 이메일과 닉네임은 DB 유니크 제약이 soft delete된 회원까지 포함하므로, 삭제된 회원의 값도 사용할 수 없다. 사전 검증은 삭제된 행까지 보는 네이티브 쿼리로 하고, 사전 검증을 통과한 동시 요청이 유니크 제약을 위반하면 제약명(`uk_member_email`, `uk_member_nickname`)으로 409(`DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`)로 변환한다.

## 공통 응답과 예외

- `CommonResponse<T>`(`code`, `name`, `message`, `data`)와 `CommonValidateResult`(`field`, `rejectValue`, `message`)는 `member-domain`의 `common/command/response`에 record로 있다. (`command/dto`는 Entity와 1:1 DTO 전용이라 `DtoRulesTest`가 검사한다)
- `CommonException`(HTTP 상태, 응답 코드, 코드명, 메시지)과 `CommonExceptionHandler`는 `member-api`의 `common.exception`에 있다. 비즈니스 예외(중복, 토큰 오류 등)는 별도 예외 클래스를 만들지 않고 `CommonException`에 코드명과 메시지를 담아 던진다. 핸들러에서 로그·알람으로 예외의 critical 여부를 한곳에서 분류하기 위해서이며, `CommonException`은 non-critical, 그 외 예외는 critical(500)로 본다.
- `CommonExceptionHandler`는 `@RestControllerAdvice("dev.choimory")`이며 다음을 처리한다.
  - `CommonException`: 예외에 지정된 상태와 값
  - `MethodArgumentNotValidException`, `ConstraintViolationException`: 400, `data`에 `CommonValidateResult` 목록
  - `HttpMessageNotReadableException`(JSON이 아니거나 읽을 수 없는 본문): 400, `data` 없음
  - `AuthenticationException`: 401, `data` 없음 (실패 사유는 노출하지 않는다)
  - `AccessDeniedException`(`@PreAuthorize` 등): 403, `data` 없음
  - 그 외 `Exception`: 500, 예외는 로그로만 남기고 응답에 메시지를 담지 않는다.
- Security 필터 단계의 예외는 `@RestControllerAdvice`에 닿지 않으므로 `UnauthorizedHandler`(401), `ForbiddenHandler`(403), 필터가 `SecurityResponseWriter`로 같은 형태의 `CommonResponse` JSON을 직접 작성한다.

## 인증과 보안

JWT(HS256) 기반 stateless 인증이며 Refresh Token과 Access Token 블랙리스트는 Redis에 저장한다.

| API | 처리 | 인증 | 설명 |
|---|---|---|---|
| `POST /v1/member/join` | `MemberCommandController` | 불필요 | 회원가입 |
| `POST /v1/login` | `LoginFilter` | 불필요 | 로그인. Access Token과 Refresh Token 발급 |
| `POST /v1/member/reissue` | `MemberCommandController` | 불필요 | Refresh Token으로 새 토큰 발급 (rotation) |
| `POST /v1/logout` | `LogoutFilter` | 불필요 | 로그아웃. Refresh Token이 인증 기준 |

- 그 외 모든 주소는 `Authorization: Bearer {accessToken}`이 필요하다. 토큰 오류는 만료 `TOKEN_EXPIRED`, 그 외 `INVALID_TOKEN`으로 401을 응답한다.
- `SecurityConfig`: CSRF, 폼 로그인, HTTP Basic, 기본 로그아웃을 끄고 stateless 세션을 쓴다. `@EnableMethodSecurity`가 켜져 있다. 필터 순서는 `JwtAuthenticationFilter` → `LoginFilter` → `LogoutFilter`이며, 필터는 스프링 빈으로 등록하면 서블릿 필터로 한 번 더 등록되므로 `SecurityConfig`에서 직접 생성한다.
- 토큰
  - Access Token 30분, Refresh Token 14일(`application-jwt.yml`). 클레임은 `sub`(회원 ID), `jti`, `iss`, `iat`, `exp`, `type`(access/refresh), `authorities`(Access Token에만). 민감한 값은 담지 않는다.
  - 서명 키는 하나(`jwt-secret`)이며 Access/Refresh Token의 종류는 `type` 클레임으로 구분한다.
  - 요청 인증은 토큰 클레임만 사용하고 DB를 조회하지 않는다. 블랙리스트 확인만 Redis를 조회하며, Redis 장애로 확인하지 못하면 인증하지 않는다(fail-closed). 이 경우 인증이 필요한 주소만 500이 되고 가입·로그인·재발급·로그아웃은 영향받지 않는다.
- Redis 키
  - `refresh:{memberId}:{jti}`: Refresh Token. 로그인할 때마다 새 jti로 저장하므로 기기별 다중 로그인이 된다. TTL은 토큰 만료까지 남은 시간.
  - `blacklist:{jti}`: 로그아웃한 Access Token. TTL은 토큰 만료까지 남은 시간.
- 로그인: `ACTIVATE` 상태의 회원만 가능하다. 없는 이메일, 틀린 비밀번호, 로그인할 수 없는 상태의 회원, 탈퇴한 회원은 구분하지 않고 같은 401을 응답한다. 요청 본문이 JSON이 아니거나 이메일, 비밀번호가 비어 있으면 인증을 시도하지 않고 400을 응답한다.
- 재발급: Refresh Token을 Redis에서 삭제하는 데 성공한 요청만 통과하므로 한 번 쓴 토큰은 다시 쓸 수 없다. 회원의 현재 상태와 권한을 다시 조회해 새 Access Token에 최신 권한을 반영하며, 로그인할 수 없는 상태가 된 회원은 재발급할 수 없다.
- 로그아웃: 본문의 Refresh Token을 폐기하고, Refresh Token과 같은 회원의 유효한 Access Token(`Authorization` 헤더, 선택)은 블랙리스트에 올린다. Access Token이 없거나 만료되었어도 로그아웃되며 이미 폐기된 토큰으로 다시 로그아웃해도 200이다. Spring Security의 `LogoutFilter`와 이름이 같은 직접 만든 필터이며 `logout()` 설정은 쓰지 않는다.
- `MemberUserDetailsService`가 이메일로 회원을 조회해 `AuthenticatedMember`(`ROLE_{권한 코드}`)를 만든다.

## API 명세서

- 모든 API는 OpenAPI 3.1 JSON으로 `docs/api/v1/choimory-dev-member-api-{domain}-v1.json`에 도메인별로 작성한다. 현재 `member`(회원가입, 토큰 재발급)와 `security`(로그인, 로그아웃)가 있다.
- `description`에는 설명을, `example`에는 예시 값을 쓰고, API마다 요청 타입별·응답 코드별 예시를 모두 작성한다. 기존 API가 바뀌면 명세서도 같이 고친다.
- Authorization 헤더처럼 요청 헤더가 있으면 `parameters`로 정의한다. (로그아웃의 선택 헤더 `Authorization`)

## DB와 실행 환경

- PostgreSQL을 사용한다. 연결 정보는 `application-datasource.yml`의 환경변수(`rdb-host`, `rdb-port`, `rdb-database`, `rdb-user`, `rdb-password`)로 받는다.
- Redis 접속 정보는 `application-redis.yml`의 환경변수(`redis-host`, `redis-port`, 선택 값 `redis-username`, `redis-password`)로 받는다. `redis-username`은 Redis ACL(6 이상)을 쓰는 환경에서만 필요하다.
- JWT 서명 키는 `application-jwt.yml`의 환경변수 `jwt-secret`(HS256, 32바이트 이상)으로 받는다. 없거나 짧으면 앱이 시작되지 않는다.
- profile: 기본은 prod이며, `prod`, `dev`, `local` 프로필 그룹은 모두 `flyway` 프로필을 포함한다. (`application-prod.yml`은 `prod` 프로필에서만 적용되고 SQL 로그를 출력하지 않는다) `datasource`, `jwt`, `redis` 프로필은 항상 포함한다.
- 스키마는 Flyway가 앱 기동 시 `db/migration`의 마이그레이션 SQL로 관리한다. (`ddl-auto: none`, 설정은 `application-flyway.yml`: `validate-on-migrate: true`, `clean-disabled: true`)
  - 마이그레이션 원본은 `docs/flyway/v1`이다. 파일명은 `V{버전}__{설명}.sql`(밑줄 두 개)이며 버전 순으로 적용된다.
  - 이미 적용된 버전은 수정하지 않는다. (체크섬 검증으로 기동이 실패한다) 변경은 `V4__...sql`처럼 새 파일을 추가한다.
  - `V1__init_schema.sql`은 기존 DDL이 적용된 DB에서도 안전하도록 `create ... if not exists`를 유지하고 DROP 문은 쓰지 않는다. COMMENT는 각 테이블의 DDL 바로 아래에 둔다.
  - `V2__timestamp_to_timestamptz.sql`은 모든 시각 컬럼(38개)을 `timestamp`에서 `timestamptz`로 바꾼다. 기존 값은 서버 시간대(`Asia/Seoul`)로 쓴 값으로 보고 `using ... at time zone 'Asia/Seoul'`로 변환한다.
  - `V3__init_authority_data.sql`은 권한 기본 데이터(`MEMBER` 회원, `ADMIN` 관리자)를 넣는다. 환경마다 같은 id를 갖도록 고정 UUID v7을 쓰고 `on conflict (code) do nothing`으로 재실행해도 안전하다. `MODERATOR`와 menu 기본 데이터는 아직 없으며 필요할 때 다음 버전으로 추가한다.
  - 설명(`__` 뒤)에 `data`가 포함된 마이그레이션은 데이터 전용 파일이다. `member-domain` 테스트는 스키마 검증이 목적이라 이 파일들을 적용하지 않는다. (`MigrationScripts`) 데이터 전용 마이그레이션을 추가할 때는 파일명 설명에 `data`를 넣는다.
  - `docs/db/v1`은 참고용이며 앱과 테스트에서 사용하지 않는다. 최신 스키마는 `docs/flyway` 기준이다.
- 기존 DDL이 적용된 DB(이력 테이블이 없는 비어 있지 않은 스키마)에 처음 붙이면 `Found non-empty schema(s) "public" but no schema history table` 오류로 기동하지 않는다. 로컬은 DB를 비우고 다시 만들고, 그 외 환경은 baseline 전략(`baseline-on-migrate`, `baseline-version`)을 정해 적용한다. (현재 설정에는 baseline이 없다)
- Flyway는 `member-api`에만 적용하며 `member-domain`에는 의존성이 없다. 서비스별로 PostgreSQL database를 따로 두고 `public` 스키마를 쓰는 구성을 기본으로 한다.

## 코드 규칙

- DTO, Request, Response는 Java record와 `@Builder(toBuilder = true)`를 사용해 불변 객체로 작성한다.
- 데이터 객체의 정적 팩토리 메소드 내부 생성은 Builder 패턴을 사용한다.
- Controller는 하나의 Service 함수만 호출한다. Service는 API 흐름을, Handler는 세부 로직을 담당한다.
- Controller는 요청 객체에 `@Valid`를 붙여 기본 검증을 하고, 존재·중복·상태 같은 비즈니스 검증은 Service가 호출하는 Handler에서 한다.
- Java 객체와 필드, 주요 함수에는 한글 Doc Comment를 작성한다.
- Java 코드 포맷은 Palantir Java Format을 사용한다. Gradle Daemon JDK가 Java 21이 아닐 수 있으므로 `gradle.properties`의 `palantir.native.formatter=true`로 native formatter를 쓴다.

## 테스트

- 테스트는 Testcontainers Postgres 16, Redis 7을 사용하며 Docker가 필요하다. (`TestcontainersConfiguration`)
- `member-domain` 테스트는 `docs/flyway`의 `V*.sql`을 버전 순으로 컨테이너 초기화 스크립트에 적용하고 `ddl-auto=validate`로 엔티티와 스키마 정합성을 검증한다. (gradle이 시스템 프로퍼티 `docs.flyway.dir`로 경로를 전달하고, 테스트의 `MigrationScripts`가 버전 숫자 순으로 정렬한다. 설명에 `data`가 포함된 데이터 전용 마이그레이션은 제외한다)
- `member-api` 테스트도 Testcontainers를 쓰며, 앱 기동 시 Flyway가 실제로 마이그레이션을 적용한다. 권한 기본 데이터(`V3`)가 들어 있으므로 회원 관련 테스트는 `member`, `member_authority` 테이블만 비운다.
- `member-api`의 `test` 태스크는 시스템 프로퍼티로 테스트 전용 `jwt-secret`, `redis-host`, `redis-port` 값을 전달한다. (Redis 연결 정보는 Testcontainers가 덮어쓴다)
- 인증이 필요한 주소와 권한(`hasRole`)을 확인하는 테스트용 컨트롤러는 별도 클래스(`TestSecurityController`)로 두고 필요한 테스트가 `@Import`한다. 테스트 클래스의 중첩 클래스는 컴포넌트 스캔에서 제외되기 때문이다.
- 주요 테스트
  - `DdlScriptTest`: unique, check, FK, 마이그레이션 SQL 재실행(멱등)
  - `MigrationScriptsTest`: 마이그레이션 파일명 규칙, 버전 순 정렬, 데이터 전용 파일 제외
  - `FlywayMigrationTest`(member-api): Flyway 이력에 V1 성공 기록, 12개 테이블 생성, 권한 기본 데이터, `ddl-auto=validate`로 엔티티와 스키마 정합성
  - `FlywayTimestamptzMigrationTest`(member-api): V1에 `Asia/Seoul` 시각을 저장한 뒤 V2 적용 후 UTC 시각으로 올바르게 변환되는지 확인
  - `FlywayProfileTest`(member-api): local, dev, prod 프로필의 flyway 활성화, 마이그레이션 SQL의 classpath 복사, prod 프로필 설정
  - `BaseEntityTest`, `MemberEntitiesTest`, `AuthorityEntitiesTest`, `AgreementEntitiesTest`: 저장, 조회, cascade, soft delete
  - `EntityRulesTest`: 모든 엔티티의 `@SQLRestriction` 선언, 클래스 레벨 Builder, `@Table` 미사용, 정적 `create` 없음
  - `DtoRulesTest`, `*DtosTest`: DTO 규칙과 변환
  - `EntityMappingTest`: 엔티티 매핑과 DDL 컬럼 일치
  - `EnumValuesTest`: enum 값과 DDL check 일치
  - `CommonResponseTest`, `CommonValidateResultTest`: 공통 응답 객체
  - `CommonExceptionTest`, `CommonExceptionHandlerTest`(member-api): 공통 예외와 예외별 응답. 예외를 던지는 테스트용 컨트롤러와 standalone MockMvc를 사용하며 Docker가 필요 없다.
  - `MemberJoinTest`(member-api): 회원가입, 입력 검증, 이메일·닉네임 중복(soft delete 포함), 동시 요청 유니크 위반의 409 변환
  - `MemberReissueTest`(member-api): 재발급 rotation, 재사용 거부, 최신 권한 반영, 정지·탈퇴 회원 거부, 만료·잘못된 토큰
  - `LoginTest`, `LogoutTest`(member-api): 로그인·로그아웃, 다중 로그인, 블랙리스트, 멱등, 계정 정보 비노출, 잘못된 요청
  - `SecurityConfigTest`, `JwtAuthenticationFilterTest`, `SecurityHandlersTest`(member-api): 열린 주소와 보호된 주소, 토큰 검증, 역할 인가, 401·403 응답
  - `JwtProviderTest`(member-api, Docker 불필요): 토큰 발급·검증, 만료, 변조, 종류 불일치, 약한 키
  - `JwtRepositoryTest`, `RedisConnectionTest`(member-api): Redis 저장·TTL·블랙리스트, 빈 username·password 접속
- JPA 테스트는 `@JpaEntityTest`를 사용하며, Auditing은 `JpaAuditingTestConfiguration`을 import한 테스트에서만 켠다. (테스트 앱에 `@EnableJpaAuditing`을 두면 `@JdbcTest`가 깨진다)

## 검증과 코드 포맷

- 전체 빌드와 테스트: `./gradlew clean build`
- 모듈별 테스트: `./gradlew :member-domain:test`, `./gradlew :member-api:test`
- 포맷 검사: `./gradlew spotlessCheck`
- 포맷 적용: `./gradlew spotlessApply`
