# 목차

- [개요](#개요)
- [로컬 기동 시 baseline 오류와 해결](#로컬-기동-시-baseline-오류와-해결)

---

# 개요

- task1에서 구현한 Flyway 적용을 IntelliJ에서 `local` 프로필로 실제 실행해 보며 확인한 내용을 기록한다.
- 로컬 DB(`localhost:5432/choimory_dev_member`, PostgreSQL 16.9)에 기동하는 중 발생한 오류와 해결 과정을 정리한다.

---

# 로컬 기동 시 baseline 오류와 해결

> 로컬 db 날리고 다시해서 잘돼.

(사용자가 IntelliJ에서 `-Dspring.profiles.active=local`로 앱을 실행한 기동 로그를 함께 붙여 넣었다)

## 답변

- 로컬 DB를 비우고 다시 실행하자 정상 기동했다.

## 발생한 오류

- 활성 프로필은 `datasource`, `local`, `flyway`였고, Flyway가 DB(`choimory_dev_member`, PostgreSQL 16.9)에 연결했다.
- 마이그레이션 1건의 검증(`Successfully validated 1 migration`)까지는 통과했다.
- 이후 `flywayInitializer` 빈 생성에서 기동이 중단되었다.

```text
FlywayException: Found non-empty schema(s) "public" but no schema history table.
Use baseline() or set baselineOnMigrate to true to initialize the schema history table.
```

## 원인

- 로컬 DB의 `public` 스키마에 이미 테이블이 있었다. 이전 sql-init 방식으로 만들어진 것이다.
- 하지만 Flyway 이력 테이블(`flyway_schema_history`)은 없었다.
- Flyway는 "비어 있지 않은데 이력도 없는 스키마"를 안전하지 않다고 보고 기동을 중단한다.
- 마이그레이션 파일이나 yml 설정의 문제가 아니다. 테스트 127개가 통과한 것은 Testcontainers의 새 DB를 쓰기 때문이다.
- plan1의 "기존 DB 처리"에서 "이미 DDL이 적용된 DB가 있으면 baseline 전략이 필요하다"고 정리한 상황이 실제로 나타난 것이다.

## 선택지

| 방법 | 내용 |
|---|---|
| 로컬 DB를 비우고 다시 만들기 | Flyway가 V1을 처음부터 적용한다. 운영과 같은 경로(빈 DB → V1)를 직접 확인할 수 있고, 로컬 데이터는 사라진다. |
| 로컬에서만 baseline 허용 | `application-local.yml`에 `baseline-on-migrate: true`, `baseline-version: 0`을 추가한다. V1이 `if not exists`라 기존 테이블에 영향이 없고 데이터는 유지된다. prod에는 넣지 않는다. |
| CLI로 baseline 1회 실행 | 설정은 건드리지 않지만 Flyway CLI 설치가 필요해 번거롭다. |

## 결정과 조치

- 로컬 DB를 비우고 다시 만드는 방법을 선택했다. (사용자가 직접 수행했으며 코드와 설정은 바꾸지 않았다)
- 결과: 다시 실행했을 때 Flyway가 V1을 적용하고 정상 기동했다.
- `baseline-on-migrate` 설정은 추가하지 않았다. 앞으로 기존 DDL이 적용된 DB(dev, prod 등)가 생기면 그때 baseline 전략을 정한다.

## 참고

- 기동 로그의 `Spring Data Elasticsearch/Redis - Could not safely identify store assignment for repository candidate` 메시지는 JPA 리포지토리를 다른 Spring Data 모듈이 후보로 검사하며 남기는 INFO 로그이며, 오류가 아니다. (`Found 4 JPA repository interfaces`로 JPA 리포지토리 4개는 정상 인식되었다)

---
