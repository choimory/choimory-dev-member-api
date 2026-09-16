# 목차

- [개요](#개요)
- [Step 2 목표](#step-2-목표)
- [이관 원칙](#이관-원칙)
- [Kotlin 원본 참조 방식](#kotlin-원본-참조-방식)
- [기준 커밋 후보](#기준-커밋-후보)
- [이관 순서](#이관-순서)
- [파일 매핑](#파일-매핑)
- [데이터 객체 변환 규칙](#데이터-객체-변환-규칙)
- [레이어별 변환 규칙](#레이어별-변환-규칙)
- [Security 변환 규칙](#security-변환-규칙)
- [미구현 및 의심 지점 처리](#미구현-및-의심-지점-처리)
- [검증 계획](#검증-계획)
- [예상 리스크](#예상-리스크)
- [완료 기준](#완료-기준)

---

# 개요

- Step 2는 기존 Kotlin 로직을 Java로 이관하는 작업이다.
- Step 1에서 Java 21 베이스 프로젝트 초기화가 완료된 뒤 별도 일정으로 진행한다.
- 이 문서는 나중에 시간이 지나도 바로 작업을 재개할 수 있도록 이관 순서와 매핑을 미리 정리한다.

---

# Step 2 목표

- 기존 Kotlin 기반 API 기능을 Java 코드로 복원한다.
- Entity, Document, DTO, Request, Response를 Java로 이관한다.
- Repository, Service, Handler, Controller를 Java로 이관한다.
- Login/Security 관련 Filter, Provider, Handler를 Java로 이관한다.
- Redis, JPA, Elasticsearch 연동이 Java 객체 기준으로 동작하도록 맞춘다.

---

# 이관 원칙

- 기존 패키지 루트는 `dev.choimory.member.api`를 기준으로 한다.
- 파일 확장자는 `.kt`에서 `.java`로 변경한다.
- Kotlin 문법을 단순 직역하기보다 `AGENTS_BACK_END.md`의 Java/Spring/JPA 규칙을 따른다.
- Entity와 DTO는 1:1 매칭한다.
- DTO, Request, Response는 불변 객체로 작성한다.
- 데이터 객체 간 변환은 정적 팩토리 메소드로 작성한다.
- Entity가 포함된 변환 책임은 Entity가 아니라 DTO, Request, Response 쪽에 둔다.
- Controller는 하나의 Service 함수만 호출한다.
- Service는 API 단위 흐름을 담당한다.
- Handler는 세부 로직 단위를 담당한다.
- Repository는 데이터 접근 책임만 가진다.

---

# Kotlin 원본 참조 방식

- Step 1에서 `src/main/kotlin`, `src/test/kotlin`은 삭제한다.
- Kotlin 원본은 저장소 내부 보관 디렉토리에 남기지 않는다.
- Step 2에서는 Step 1 이전 Git 커밋에서 Kotlin 원본을 참조한다.
- Step 2 시작 시 기준 커밋 해시를 먼저 확인한다.
- 파일 단위 확인은 `git show <기준커밋>:<파일경로>` 방식으로 진행한다.
- 필요한 경우 특정 파일만 임시로 확인하거나 복원하되, 최종 결과는 Java 파일로 작성한다.

## Step 2 시작 전 확인 사항

- Step 1 이전 Kotlin 코드가 포함된 기준 커밋 해시를 확인한다.
- 기준 커밋에 `src/main/kotlin`, `src/test/kotlin`이 존재하는지 확인한다.
- 기준 커밋을 확인할 수 없으면 Step 2 진행이 어려울 수 있다.

---

# 기준 커밋 후보

- Git 확인 결과 현재 HEAD는 `bee303584cfaf9d0ba71668df79687de1f2122f8`다.
- 현재 HEAD에는 Kotlin 파일 41개가 모두 추적된 상태로 포함되어 있다.
- Kotlin 소스 쪽 uncommitted 변경은 없다.
- 따라서 Step 2에서 Kotlin 원본을 참조할 기준 커밋 후보는 `bee303584cfaf9d0ba71668df79687de1f2122f8`다.

## 조회 확인

- `git show HEAD:src/main/kotlin/dev/choimory/member/api/ChoimoryDevMemberApiApplication.kt` 조회가 가능했다.
- `git show HEAD:src/main/kotlin/dev/choimory/member/api/member/v1/command/domain/entity/MemberEntity.kt` 조회가 가능했다.

## 주의 사항

- 이후 Step 1 전 문서 변경사항을 커밋한다면, 그 새 커밋도 Kotlin 원본을 포함하므로 기준 커밋으로 사용할 수 있다.
- Step 2 시작 시에는 실제 기준 커밋을 다시 확정한다.

---

# 이관 순서

1. 공통 도메인 객체를 이관한다.
2. 공통 예외 객체와 예외 핸들러를 이관한다.
3. Member command 도메인 객체를 이관한다.
4. Member query 도메인 객체를 이관한다.
5. Repository를 이관한다.
6. Handler를 이관한다.
7. Service를 이관한다.
8. Controller를 이관한다.
9. Config를 이관한다.
10. Login/Security 객체를 이관한다.
11. 테스트 보조 클래스를 이관한다.
12. 컴파일과 동작 검증을 진행한다.

---

# 파일 매핑

## Application

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `src/main/kotlin/dev/choimory/member/api/ChoimoryDevMemberApiApplication.kt` | `src/main/java/dev/choimory/member/api/ChoimoryDevMemberApiApplication.java` | 1 | Step 1에서 최소 실행 클래스로 먼저 생성 |

## Common

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `common/domain/entity/CommonDateEntity.kt` | `common/domain/entity/CommonDateEntity.java` | 1 | JPA Auditing, `@MappedSuperclass` |
| `common/domain/document/CommonDateDocument.kt` | `common/domain/document/CommonDateDocument.java` | 1 | ES Document 내부 값 객체 |
| `common/domain/dto/CommonValidateResult.kt` | `common/domain/dto/CommonValidateResult.java` | 1 | Validation 응답 DTO |
| `common/domain/response/CommonResponse.kt` | `common/domain/response/CommonResponse.java` | 1 | 공통 응답 제네릭 |
| `common/exception/CommonException.kt` | `common/exception/CommonException.java` | 2 | RuntimeException 상속 |
| `common/exception/CommonExceptionHandler.kt` | `common/exception/CommonExceptionHandler.java` | 3 | Kotlin logging 제거, SLF4J 사용 |
| `common/util/TimeUnitUtil.kt` | `common/util/TimeUnitUtil.java` | 3 | companion object를 static method로 변환 |

## Member Command

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `member/v1/command/domain/entity/MemberEntity.kt` | `member/v1/command/domain/entity/MemberEntity.java` | 1 | JPA Entity, Lombok 적용 |
| `member/v1/command/domain/dto/MemberEntityDto.kt` | `member/v1/command/domain/dto/MemberEntityDto.java` | 1 | Entity 변환 정적 팩토리 |
| `member/v1/command/domain/request/CreateMemberRequest.kt` | `member/v1/command/domain/request/CreateMemberRequest.java` | 1 | Validation, MultipartFile |
| `member/v1/command/domain/request/VerifyMemberRequest.kt` | `member/v1/command/domain/request/VerifyMemberRequest.java` | 1 | Validation |
| `member/v1/command/domain/response/CreateMemberResponse.kt` | `member/v1/command/domain/response/CreateMemberResponse.java` | 1 | 불변 응답 |
| `member/v1/command/domain/response/VerifyMemberResponse.kt` | `member/v1/command/domain/response/VerifyMemberResponse.java` | 1 | 불변 응답 |
| `member/v1/command/repository/MemberCommandRepository.kt` | `member/v1/command/repository/MemberCommandRepository.java` | 2 | JpaRepository |
| `member/v1/command/service/MemberCommandHandler.kt` | `member/v1/command/service/MemberCommandHandler.java` | 3 | Redis 캐스팅 주의 |
| `member/v1/command/service/MemberCommandService.kt` | `member/v1/command/service/MemberCommandService.java` | 4 | Transaction 유지 |
| `member/v1/command/controller/MemberCommandController.kt` | `member/v1/command/controller/MemberCommandController.java` | 5 | `@RequestBody` 여부 결정 |

## Member Query

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `member/v1/query/domain/document/MemberDocument.kt` | `member/v1/query/domain/document/MemberDocument.java` | 1 | Elasticsearch Document |
| `member/v1/query/domain/dto/MemberDocumentDto.kt` | `member/v1/query/domain/dto/MemberDocumentDto.java` | 1 | `from()` TODO 처리 여부 결정 |
| `member/v1/query/domain/request/FindMemberRequest.kt` | `member/v1/query/domain/request/FindMemberRequest.java` | 1 | 현재 빈 클래스 |
| `member/v1/query/domain/response/FindMemberResponse.kt` | `member/v1/query/domain/response/FindMemberResponse.java` | 1 | 현재 빈 클래스 |
| `member/v1/query/repository/MemberQueryRepository.kt` | `member/v1/query/repository/MemberQueryRepository.java` | 2 | ElasticsearchRepository |
| `member/v1/query/repository/MemberQueryClient.kt` | `member/v1/query/repository/MemberQueryClient.java` | 2 | ElasticsearchClient 래퍼 |
| `member/v1/query/service/MemberQueryHandler.kt` | `member/v1/query/service/MemberQueryHandler.java` | 3 | 로그인 조회/검증 |
| `member/v1/query/service/MemberQueryService.kt` | `member/v1/query/service/MemberQueryService.java` | 4 | query service |
| `member/v1/query/controller/MemberQueryController.kt` | `member/v1/query/controller/MemberQueryController.java` | 5 | 현재 엔드포인트 없음 |

## Login, Token, Security

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `token/v1/domain/TokenDetails.kt` | `token/v1/domain/TokenDetails.java` | 1 | `of()` 정적 팩토리 |
| `login/v1/domain/request/LoginRequest.kt` | `login/v1/domain/request/LoginRequest.java` | 1 | package 불일치 결정 필요 |
| `login/v1/domain/response/LoginResponse.kt` | `login/v1/domain/response/LoginResponse.java` | 1 | 불변 응답 |
| `login/v1/security/LoginAuthProvider.kt` | `login/v1/security/LoginAuthProvider.java` | 3 | null 처리 |
| `login/v1/security/LoginProcessFilter.kt` | `login/v1/security/LoginProcessFilter.java` | 3 | LoginRequest import 결정 |
| `login/v1/security/LoginSuccessHandler.kt` | `login/v1/security/LoginSuccessHandler.java` | 3 | 토큰 TODO 유지 여부 |
| `login/v1/security/LoginFailureHandler.kt` | `login/v1/security/LoginFailureHandler.java` | 3 | 공통 응답 제네릭 |

## Config

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `config/AppConfig.kt` | `config/AppConfig.java` | 2 | ObjectMapper, PasswordEncoder |
| `config/RedisConfig.kt` | `config/RedisConfig.java` | 2 | RedisTemplate serializer |
| `config/ElasticSearchConfig.kt` | `config/ElasticSearchConfig.java` | 2 | ES client |
| `config/SecurityConfig.kt` | `config/SecurityConfig.java` | 4 | SecurityFilterChain |

## Test

| Kotlin 파일 | Java 파일 | 우선순위 | 비고 |
|---|---|---:|---|
| `src/test/kotlin/dev/choimory/member/api/TestcontainersConfiguration.kt` | `src/test/java/dev/choimory/member/api/TestcontainersConfiguration.java` | 6 | Docker 필요 가능 |
| `src/test/kotlin/dev/choimory/member/api/ChoimoryDevMemberApiApplicationTests.kt` | `src/test/java/dev/choimory/member/api/ChoimoryDevMemberApiApplicationTests.java` | 6 | contextLoads |
| `src/test/kotlin/dev/choimory/member/api/TestChoimoryDevMemberApiApplication.kt` | `src/test/java/dev/choimory/member/api/TestChoimoryDevMemberApiApplication.java` | 6 | Testcontainers 실행 main |

---

# 데이터 객체 변환 규칙

## Entity

- Lombok `@Getter`를 사용한다.
- JPA 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 사용한다.
- 필드는 private로 선언한다.
- Entity 수정이 필요하면 Entity 내부 메소드로 작성한다.
- Entity에는 DTO 변환 메소드를 작성하지 않는다.

## DTO

- 불변 객체로 작성한다.
- Lombok `@Getter`를 사용한다.
- 생성자는 private 또는 public 정책을 파일별로 맞춘다.
- Entity에서 DTO로의 변환은 DTO의 정적 팩토리 메소드로 작성한다.
- Redis JSON 역직렬화가 필요한 DTO는 Jackson 생성자 정책을 별도 검토한다.

## Request

- API 요청 객체는 불변으로 작성한다.
- JSON 요청 객체라면 `@JsonCreator`, `@JsonProperty` 적용을 검토한다.
- Validation 어노테이션을 유지한다.
- `MultipartFile` 포함 요청은 `@RequestBody`가 아니라 multipart 처리 가능성을 따로 검토한다.

## Response

- API 응답 객체는 불변으로 작성한다.
- DTO에서 Response로의 변환은 Response의 정적 팩토리 메소드로 작성한다.
- 단순 응답은 생성자 기반으로 유지할 수 있다.

## Java record 여부

- Java 21에서는 record를 사용할 수 있다.
- 다만 현재 백엔드 규칙 예시는 Lombok 기반 class를 사용한다.
- 프로젝트 규칙 일관성을 우선하면 Lombok class가 기본 선택지다.
- record 사용 여부는 Step 2 시작 전에 별도 결정한다.

---

# 레이어별 변환 규칙

## Repository

- Kotlin interface를 Java interface로 변환한다.
- `JpaRepository<MemberEntity, String>`와 `ElasticsearchRepository<MemberDocument, String>` 타입을 유지한다.
- Optional 반환 타입을 유지한다.

## Handler

- 생성자 주입을 사용한다.
- Lombok `@RequiredArgsConstructor` 사용을 검토한다.
- Redis safe cast는 Java에서 `instanceof` 패턴 또는 명시 캐스팅으로 처리한다.
- Kotlin `random()`은 Java의 `ThreadLocalRandom` 또는 `SecureRandom`으로 대체한다.

## Service

- `@Service`와 `@Transactional`을 유지한다.
- API 흐름 단위 메소드를 유지한다.
- Kotlin `requireNotNull`은 Java에서 명시적인 null 체크와 예외 throw로 바꾼다.

## Controller

- `@RestController`, `@RequestMapping`, `@Validated`를 유지한다.
- Controller 메소드는 하나의 Service 메소드만 호출한다.
- JSON 요청이면 `@RequestBody` 추가 여부를 결정한다.
- multipart 요청이면 `@ModelAttribute` 또는 multipart 처리 방식을 결정한다.

---

# Security 변환 규칙

- `AuthenticationProvider` 구현을 Java class로 변환한다.
- `AbstractAuthenticationProcessingFilter` 상속 구조를 유지한다.
- `AuthenticationSuccessHandler`, `AuthenticationFailureHandler` 구현을 유지한다.
- Kotlin nullable 처리 대신 Java 명시 null 체크를 사용한다.
- `ObjectMapper.readValue()` 대상 LoginRequest package를 먼저 확정한다.
- 성공/실패 응답은 `CommonResponse` Java 타입 기준으로 맞춘다.

---

# 미구현 및 의심 지점 처리

## LoginRequest package 불일치

- 현재 파일 경로는 `dev/choimory/member/api/login/v1/domain/request`다.
- 현재 package는 `io.choimory.member.external.api.login.domain.request`다.
- `LoginProcessFilter`도 이 package를 import 중이다.
- Step 2 시작 전에 외부 API 패키지 의도인지, 단순 오타인지 결정한다.

## RequestBody 누락

- `MemberCommandController` 요청 파라미터에 `@RequestBody`가 없다.
- JSON 요청 API라면 추가하는 방향이 자연스럽다.
- `CreateMemberRequest`에 `MultipartFile`이 있으므로 회원가입 요청이 JSON인지 multipart인지 먼저 결정한다.

## TODO

- `MemberDocumentDto.from()`은 현재 TODO다.
- `LoginSuccessHandler`의 access token, refresh token 생성이 TODO다.
- `MemberCommandHandler.generateToken()`도 TODO다.
- Step 2의 목표가 기능 복원이면 기존 TODO는 그대로 옮길 수 있다.
- Step 2의 목표가 정상 동작 복원이면 TODO 구현을 별도 하위 작업으로 분리한다.

---

# 검증 계획

- 데이터 객체 이관 후 `compileJava`를 확인한다.
- Repository와 config 이관 후 `compileJava`를 다시 확인한다.
- Service, Handler, Controller 이관 후 `compileJava`를 다시 확인한다.
- Security 이관 후 `compileJava`를 다시 확인한다.
- 테스트 이관 후 `test` 실행 여부를 별도 허락받고 진행한다.
- Testcontainers 테스트는 Docker 환경이 필요하므로 실행 전 확인한다.

---

# 예상 리스크

- Spring Boot snapshot 버전 사용으로 의존성 해석이 불안정할 수 있다.
- 기존 리소스 설정이 DB, Redis, Elasticsearch 환경변수에 의존해 부팅 실패할 수 있다.
- Redis JSON 역직렬화가 Kotlin data class에서 Java 불변 class로 바뀌며 실패할 수 있다.
- Elasticsearch Document 생성자와 필드 매핑이 Java에서 달라질 수 있다.
- JPA Entity 기본 생성자와 접근 제어자를 잘못 설정하면 런타임 오류가 날 수 있다.
- TODO 로직을 그대로 이관하면 컴파일은 되어도 실제 기능은 완성되지 않는다.
- `LoginRequest` package 불일치를 정리하지 않으면 import와 패키지 구조가 계속 혼란스러울 수 있다.

---

# 완료 기준

- 기존 Kotlin 로직에 대응되는 Java 파일이 생성되어 있다.
- Kotlin 소스 없이 Java 소스만으로 컴파일된다.
- 기존 API 엔드포인트 구조가 Java Controller로 복원되어 있다.
- JPA Repository와 Elasticsearch Repository가 Java 도메인 객체를 기준으로 동작한다.
- Login/Security 흐름이 Java 코드로 복원되어 있다.
- 미구현 TODO와 의심 지점의 처리 결과가 문서에 기록되어 있다.
