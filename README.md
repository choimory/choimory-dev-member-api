# choimory-dev-member-api

## 개요

- Choimory 개발 회원 API 프로젝트다.
- Java 21 기반 Spring Boot 회원 API 서버다.
- 기존 Kotlin 소스를 Java 코드로 이관한 상태다.
- 회원 가입, 회원 인증, 로그인 인증 흐름을 제공한다.
- RDB는 회원 command 모델, Elasticsearch는 로그인 조회용 query 모델로 사용한다.

## 기술 구성

- Language
  - Java 21
- Build
  - Gradle Groovy DSL
  - Gradle Wrapper
- Framework
  - Spring Boot
  - Spring Web
  - Spring Security
  - Spring Validation
  - Spring Data JPA
  - Spring Data Elasticsearch
  - Spring Data Redis
- Library
  - Elasticsearch Java API Client
  - Jackson Databind
  - UUID Creator
  - Lombok
  - PostgreSQL Driver
- Test
  - JUnit5
  - Spring Boot Test
  - Spring Security Test
  - Testcontainers
  - Testcontainers PostgreSQL

## 프로젝트 구조

```text
.
├── build.gradle
├── settings.gradle
├── src
│   ├── main
│   │   ├── java/dev/choimory/member/api
│   │   │   ├── ChoimoryDevMemberApiApplication.java
│   │   │   ├── common
│   │   │   │   ├── domain
│   │   │   │   ├── exception
│   │   │   │   └── util
│   │   │   ├── config
│   │   │   ├── login/v1
│   │   │   ├── member/v1
│   │   │   │   ├── command
│   │   │   │   └── query
│   │   │   └── token/v1
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-datasource.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── sql
│   └── test
│       └── java/dev/choimory/member/api
└── README.md
```

## 애플리케이션

- 메인 클래스는 `dev.choimory.member.api.ChoimoryDevMemberApiApplication`이다.
- `@SpringBootApplication`을 사용한다.
- JPA Auditing을 위해 `@EnableJpaAuditing`을 사용한다.

## 아키텍처

- `common`
  - 공통 응답, 공통 validation 결과, 공통 예외, 공통 일시 Entity/Document를 제공한다.
- `member.v1.command`
  - 회원 가입과 회원 인증 쓰기 흐름을 담당한다.
  - `MemberEntity`는 PostgreSQL 저장 모델이다.
  - `MemberEntityDto`는 Entity와 1:1로 매칭되는 도메인 DTO다.
  - `MemberCommandController`는 command API 요청을 받고 Service 한 개만 호출한다.
  - `MemberCommandService`는 API 흐름을 담당한다.
  - `MemberCommandHandler`는 비밀번호 암호화, Redis 인증 대기 저장, 회원 저장 같은 세부 로직을 담당한다.
- `member.v1.query`
  - 로그인에 필요한 회원 조회 흐름을 담당한다.
  - `MemberDocument`는 Elasticsearch 조회 모델이다.
  - `MemberDocumentDto`는 Document 기반 로그인 검증 결과 DTO다.
  - `MemberQueryRepository`는 Elasticsearch Repository다.
- `login.v1`
  - `/login` 요청 payload를 Spring Security 인증 객체로 변환한다.
  - 로그인 성공/실패 응답은 `CommonResponse` 형식으로 작성한다.
- `token.v1`
  - 토큰 Claim에 사용할 `TokenDetails`를 제공한다.
- `config`
  - ObjectMapper, PasswordEncoder, RedisTemplate, ElasticsearchClient, SecurityFilterChain을 설정한다.

## API 흐름

### 회원가입

1. `POST /v1/member/sign-up` 요청을 받는다.
2. 비밀번호를 BCrypt로 암호화한다.
3. UUID와 6자리 인증 코드를 생성한다.
4. 인증 대기 회원 정보를 Redis에 3분 동안 저장한다.
5. `uuid`, `verifyCode`를 응답한다.

### 회원 인증

1. `POST /v1/member/verify` 요청을 받는다.
2. Redis에서 `uuid:verifyCode` 키로 인증 대기 회원 정보를 조회한다.
3. 요청 이메일과 Redis 회원 이메일이 일치하는지 확인한다.
4. PostgreSQL에 회원 정보를 저장한다.
5. 현재는 기존 Kotlin TODO 상태를 따라 placeholder 토큰을 응답한다.

### 로그인

1. `POST /login` 요청 payload를 `LoginProcessFilter`가 읽는다.
2. `LoginAuthProvider`가 이메일과 비밀번호를 검증한다.
3. 회원 정보는 Elasticsearch의 `MemberDocument`에서 조회한다.
4. 비밀번호는 `PasswordEncoder.matches()`로 검증한다.
5. 현재는 기존 Kotlin TODO 상태를 따라 placeholder 토큰을 응답한다.

## 리소스

- `application.yml`은 기본 애플리케이션 설정을 가진다.
- `application-datasource.yml`은 PostgreSQL, Elasticsearch, Redis 연결 설정을 가진다.
- `application-local.yml`, `application-dev.yml`, `application-prod.yml`은 profile별 JPA와 logging 설정을 가진다.
- `src/main/resources/sql`에는 SQL 초기화 파일이 있다.

## 코드 규칙

- DTO, Request, Response는 불변 객체로 작성한다.
- 데이터 객체 변환은 변환 결과 객체의 정적 팩토리 메소드로 작성한다.
- Entity에는 DTO 변환 메소드를 작성하지 않는다.
- Controller는 하나의 Service 함수만 호출한다.
- Service는 API 흐름을 담당한다.
- Handler는 세부 로직을 담당한다.
- Java 객체와 필드, 주요 함수에는 한글 Doc Comment를 작성한다.

## 주의 사항

- `application.yml`은 `datasource` profile을 include하므로 실행 시 DB, Redis, Elasticsearch 환경변수가 필요하다.
- 외부 연결 환경변수가 없으면 애플리케이션 부팅은 실패할 수 있다.
- `MemberDocumentDto.from()`은 기존 Kotlin TODO였지만 Java 이관 과정에서 Document와 1:1 필드 매핑으로 구현했다.
- 토큰 생성과 Redis 토큰 등록은 기존 Kotlin TODO 상태를 유지해 placeholder 값을 응답한다.
- `LoginRequest`는 기존 Kotlin 파일의 package 불일치를 정리해 `dev.choimory.member.api.login.v1.domain.request`로 이관했다.
- Elasticsearch Java API Client는 Spring Boot/Spring Data가 관리하는 버전을 사용한다.

## 검증

- `./gradlew compileJava`
- `./gradlew compileTestJava`
- `./gradlew test`
