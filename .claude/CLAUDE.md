# 프로젝트 초기화 정보

## 프로젝트 개요

**choimory-dev-member-api**는 CQRS 패턴을 적용한 멤버 관리 API 서비스입니다.

## 기술 스택

### 언어 및 프레임워크
- **언어**: Kotlin 1.9.25
- **JDK**: Java 17
- **프레임워크**: Spring Boot 3.5.4-SNAPSHOT
- **빌드 도구**: Gradle (Kotlin DSL)

### 주요 라이브러리
- Spring Security (JWT 인증)
- Spring Data JPA (PostgreSQL)
- Spring Data Elasticsearch
- Spring Data Redis
- Spring Validation
- Kotlin Logging
- Querydsl
- Ktlint (코드 스타일)
- Testcontainers (테스트)

## 아키텍처

### CQRS 패턴
- **Command**: PostgreSQL - 데이터 쓰기 작업
- **Query**: Elasticsearch - 데이터 읽기 작업
- **Cache**: Redis - 캐싱

### 패키지 구조
```
dev.choimory.member.api
├── config          # 설정 클래스
├── token          # 토큰 관련 (JWT)
└── member         # 멤버 도메인
    └── v1
        ├── command    # Command 영역 (쓰기)
        └── query      # Query 영역 (읽기)
```

## 데이터 모델

### 정규화 DB (PostgreSQL)
- Member - 회원
- MemberSuspension - 회원 정지
- Grade - 등급
- GradeRole - 등급별 권한
- Follow - 팔로우

### 역정규화 Index (Elasticsearch)
- Member - 회원 검색 인덱스

## 개발 환경 설정

### 필수 설치 항목
- JDK 17
- Docker & Docker Compose (PostgreSQL, Elasticsearch, Redis)

### 빌드 및 실행
```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test

# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 수정
./gradlew ktlintFormat
```

## 코드 컨벤션

- Ktlint 규칙 준수
- Entity, MappedSuperclass, Embeddable은 allOpen 적용
- 한국어 주석 및 문서화

## 인증 방식

- JWT 기반 토큰 인증
- Spring Security 적용

## 테스트

- JUnit5
- Testcontainers (PostgreSQL)
- Spring Security Test
