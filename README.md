# choimory-dev-member-api

## 개요

- Choimory 개발 회원 API 프로젝트다.
- 현재는 Java 21 기반 Spring Boot 빈 프로젝트 구조다.
- 기존 회원 API 도메인 로직은 현재 소스 트리에 존재하지 않는다.
- 회원 API 도메인 로직, Entity, DTO, Controller, Service, Security 흐름은 이후 Java 코드로 별도 이관한다.

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
├── gradlew
├── gradlew.bat
├── gradle
│   └── wrapper
├── src
│   ├── main
│   │   ├── java
│   │   │   └── dev/choimory/member/api
│   │   │       └── ChoimoryDevMemberApiApplication.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── application-datasource.yml
│   │       ├── application-local.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── sql
│   └── test
└── README.md
```

## 애플리케이션

- 메인 클래스는 `dev.choimory.member.api.ChoimoryDevMemberApiApplication`이다.
- `@SpringBootApplication`을 사용한다.
- 이후 JPA Entity 이관을 고려해 `@EnableJpaAuditing`을 유지한다.

## 리소스

- `application.yml`은 기본 애플리케이션 설정을 가진다.
- `application-datasource.yml`은 PostgreSQL, Elasticsearch, Redis 연결 설정을 가진다.
- `application-local.yml`, `application-dev.yml`, `application-prod.yml`은 profile별 JPA와 logging 설정을 가진다.
- `src/main/resources/sql`에는 SQL 초기화 파일이 있다.

## 주의 사항

- 현재 프로젝트는 빈 Java 21 베이스 프로젝트 상태다.
- 기존 API 엔드포인트와 도메인 로직은 아직 Java로 이관되지 않았다.
- 리소스 설정은 DB, Redis, Elasticsearch 환경변수에 의존한다.
- 외부 연결 환경변수가 없으면 애플리케이션 부팅은 실패할 수 있다.
- 현재 확인된 기준 검증은 `./gradlew test`이며, 테스트 소스가 없어 `test NO-SOURCE` 상태로 성공했다.
