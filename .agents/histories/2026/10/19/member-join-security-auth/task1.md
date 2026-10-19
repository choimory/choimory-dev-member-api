# 목차

- [개요](#개요)
- [회원가입 API 및 중복 체크 구현](#회원가입-api-및-중복-체크-구현)
  - [답변](#답변)
  - [작업 내용](#작업-내용)
  - [테스트](#테스트)
  - [검증 결과](#검증-결과)
  - [남은 사항](#남은-사항)
- [추가 요구사항 반영](#추가-요구사항-반영)
  - [답변](#답변-1)
  - [Request 입력값 제약](#request-입력값-제약)
  - [예외 정리](#예외-정리)
  - [권한 기본 데이터](#권한-기본-데이터)
  - [도메인 테스트](#도메인-테스트)
  - [테스트 변경](#테스트-변경)
  - [검증 결과](#검증-결과-1)
  - [남은 사항](#남은-사항-1)
- [Handler의 private 메서드 인라인](#handler의-private-메서드-인라인)
  - [답변](#답변-2)
- [동시 요청 대비 catch 유지](#동시-요청-대비-catch-유지)
  - [답변](#답변-3)

---

# 개요

- plan1.md에서 논의한 회원가입 API를 구현한다. (`POST /v1/member/join`)
- 이메일, 닉네임 중복 검증은 soft delete된 회원까지 포함하고, 동시 요청으로 인한 유니크 제약 위반은 409로 변환한다.
- 약관 검증과 이메일 인증 queue 전송은 TODO로 남긴다.
- Handler는 기능별 클래스로 나누지 않고 `MemberCommandHandler`에 둔다.
- 참고: plan1.md, `.agents/rules/back-end/code-convention-springboot.md`, README

---

# 회원가입 API 및 중복 체크 구현

> 네 시작해줘. 저장까지 포함, 테스트도 같이

## 답변

회원가입 API와 중복 체크 검증을 구현하고 테스트까지 작성했다. `./gradlew clean build`가 통과했고 `member-api` 테스트 31개가 모두 성공했다. 작업 시작 시 `calendar.md`를 TASK로 변경했다.

## 작업 내용

### API

- `POST /v1/member/join`. 컨트롤러의 경로와 `CommonResponse` 반환 형태는 기존에 작성되어 있던 것을 유지했다. 성공 시 201을 반환한다.
- `MemberJoinRequest`: `email`, `password`, `nickname`, `introduce`
  - 비밀번호는 8~64자이며 영문, 숫자, 특수문자를 모두 포함해야 한다.
  - 닉네임은 2~20자, 자기소개는 500자 이하이다. 길이 제한은 임의로 정한 값이다.
  - `toEntity(encodedPassword)`에서 상태를 `ACTIVATE`로 둔다.
- `MemberJoinResponse`: `id`, `email`, `nickname`, `status`. 비밀번호는 포함하지 않는다. `from(MemberDto)`로 변환한다.

### 중복 검증

- `MemberRepository`: 삭제된 회원까지 보는 네이티브 쿼리 `existsByEmailIncludingDeleted`, `existsByNicknameIncludingDeleted`
- `MemberCommandHandler.validateJoin`: 이메일, 닉네임 순으로 검사하고 중복이면 `MemberDuplicateException`을 던진다.
- `MemberDuplicateException`: `CommonException`을 상속한 409 예외. `DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`과 유니크 제약명 상수(`uk_member_email`, `uk_member_nickname`)를 가진다.

### 저장

- `MemberCommandHandler.join`: `PasswordEncoder`로 비밀번호를 암호화하고, 권한을 `EntityManager.getReference`로 참조해 `MemberAuthority`를 붙인 뒤 `saveAndFlush`로 저장한다. 유니크 제약 위반을 이 시점에 확인하기 위해 즉시 반영한다.
- `MemberCommandService.join`(`@Transactional`): 중복 검증, 기본 권한(`MEMBER`) 조회, 회원 저장, 응답 변환 순서로 진행한다. 약관 검증, 약관 동의 이력 저장, 이메일 인증 메일 queue 전송은 `// TODO`로 남겼다.
- `AuthorityCommandHandler.findByCode`, `AuthorityRepository.findByCode`: 권한 코드로 조회한다. 없으면 `IllegalStateException`(500)이다.
- `PasswordEncoderConfig`: `BCryptPasswordEncoder` 빈

### 동시 요청 처리

- `CommonExceptionHandler`에 `DataIntegrityViolationException` 처리를 추가했다. Hibernate의 제약 이름으로 이메일, 닉네임 중복을 구분해 409로 변환하고, 제약 이름을 못 얻으면 DB 오류 메시지에서 찾는다. 알 수 없는 제약 위반은 500이다.

### 컨벤션과 다른 부분

- 다른 도메인(권한)의 엔티티를 조회 없이 참조하기 위해 `MemberCommandHandler`에서 `EntityManager.getReference`를 썼다. Member Handler가 `AuthorityRepository`를 직접 쓰지 않기 위한 선택이다.

## 테스트

- `MemberJoinTest`(신규, Testcontainers PostgreSQL): 정상 가입(ACTIVATE, 암호화된 비밀번호, MEMBER 권한), 이메일 중복, 닉네임 중복, soft delete된 이메일과 닉네임 재사용 불가, 검증 실패 400, 유니크 위반의 409 변환
- `CommonExceptionHandlerTest`: 알 수 없는 제약 위반 500, 이메일 제약 위반 409 추가
- 테스트에서 비밀번호 `weak`가 길이와 패턴 규칙을 모두 위반해 검증 오류가 4건 나오는 것을 처음에 3건으로 기대해 실패했다. 필드 이름 검증으로 고쳤다.
- Security 설정이 아직 없어 `MemberJoinTest`는 `@AutoConfigureMockMvc(addFilters = false)`로 필터를 끄고 가입 흐름만 검증한다.
- 포맷은 `./gradlew spotlessApply`로 적용했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 31개 중 실패 0건

## 남은 사항

- `SecurityConfig`가 없어 실제 호출은 Spring Security 기본 설정에 막힌다. 다음 단계에서 `/v1/member/join`을 열어야 한다.
- 초기 데이터(DML)가 없어 운영 DB에는 `MEMBER` 권한 행이 없다. 이 상태에서 가입하면 500이 난다. 초기 데이터용 `V3` 마이그레이션이 필요하다.
- 약관 검증, 약관 동의 이력 저장, 이메일 인증 queue 전송은 TODO이다.
- README에는 아직 반영하지 않았다.
- 다음 작업 순서: Security 기본 설정과 JWT, 로그인(토큰 발급), 로그아웃, 토큰 재발급

---

# 추가 요구사항 반영

> 1로 시작

## 답변

plan1.md의 "추가3"~"추가5"에서 정한 결정을 반영했다. 파일명 규칙은 설명에 `data`를 포함하는 방식(1번)으로 정했고, 앞의 "회원가입 API 및 중복 체크 구현" 섹션의 일부를 바꾸거나 되돌렸다. 앞 섹션의 남은 사항 중 권한 기본 데이터(`V3`)는 이 섹션에서 처리했다.

## Request 입력값 제약

- 이메일: `@Email`에 정규식 `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`를 추가해 `a@b`처럼 점이 없는 도메인을 막는다. 영문 도메인만 허용한다.
- 닉네임: `^[가-힣A-Za-z0-9]{3,15}$`로 교체하고 `@Size`는 뺐다. 특수문자를 제외한 한글, 영문, 숫자만 3~15자이다.
- 비밀번호(8~64자, 영문·숫자·특수문자 모두 포함)와 자기소개(500자 이하)는 그대로 둔다.

## 예외 정리

- `MemberDuplicateException`을 삭제했다. 별도 예외를 만들지 않고 `CommonException`에 메시지를 담아 던진다.
  - 이유: 예외 핸들러에 로그와 알람을 붙여 critical 여부를 모니터링할 계획이다. 비즈니스 예외를 `CommonException`으로 통일하면 non-critical로 일괄 분류할 수 있고, 예외 클래스가 늘어나 복잡해지는 것도 막는다.
  - 반대로 `CommonException`이 아닌 예외는 일반 `Exception` 핸들러로 가서 error 로그와 500이 되므로 critical로 분류된다. 중복 가입은 non-critical, `MEMBER` 권한 행이 없어 나는 `IllegalStateException`은 critical이다.
- `MemberCommandHandler`
  - `validateJoin`: 중복이면 `new CommonException(HttpStatus.CONFLICT, 409, "DUPLICATE_EMAIL" 또는 "DUPLICATE_NICKNAME", 메시지)`를 던진다.
  - `join`: `saveAndFlush`가 던지는 `DataIntegrityViolationException`을 잡아 위반한 제약명(`uk_member_email`, `uk_member_nickname`)으로 같은 `CommonException`(409)으로 변환한다. 알 수 없는 제약 위반은 그대로 던져 critical(500)로 간다.
  - `violatedConstraint`: Hibernate 원인 체인에서 제약명을 찾고, 못 찾으면 DB 오류 메시지에서 알려진 제약명을 찾는다.
- `CommonExceptionHandler`: 앞 섹션에서 추가했던 `DataIntegrityViolationException` 처리를 제거해 원래대로 되돌렸다. member 패키지 import도 없어졌다.

## 권한 기본 데이터

- `docs/flyway/v1/V3__init_authority_data.sql`: 고정 UUID v7로 `MEMBER`(회원), `ADMIN`(관리자)을 insert하고 `on conflict (code) do nothing`을 붙였다. `MODERATOR`는 넣지 않았고, menu는 메뉴가 구체화될 때 추가하기로 하고 보류했다.
- `docs/db/v1/v1-choimory-dev-member-dml.sql`: 참고용 DML에도 같은 내용을 반영했다.
- UUID v7 값은 작업 시점의 시각으로 만든 값이며, 환경마다 같은 id가 되도록 고정했다.

## 도메인 테스트

- `member-domain` 테스트는 `docs/flyway`의 `V*.sql`을 모두 적용하는데, V3의 `MEMBER`, `ADMIN` 행이 들어가면 같은 코드를 직접 insert하는 테스트(`AuthorityEntitiesTest`, `DdlScriptTest` 등)가 유니크 위반으로 깨진다.
- 대응: `MigrationScripts.files()`가 파일명의 설명(`__` 뒤)에 `data`가 포함된 파일을 제외한다. 스키마 검증이 목적인 도메인 테스트에는 기본 데이터가 필요 없다. Flyway와 `member-api`의 복사 설정은 그대로이므로 앱은 V3까지 모두 적용한다.
- 이 규칙은 도메인 테스트가 데이터 파일을 구분하기 위한 장치일 뿐이어서 설명에 `data`를 넣는 정도로만 정했다.

## 테스트 변경

- `MemberJoinTest`
  - 직접 넣던 `MEMBER` 권한 insert를 없애고 V3 seed를 쓴다. `truncate`에서 authority를 제외했다.
  - 이메일 `a@b` 거부, 닉네임 경계(3자·15자 통과, 2자·16자·`_`·`!`·공백 거부, 한글 허용) 테스트를 추가했다.
  - 동시 요청 변환 테스트는 `MemberCommandHandler.join`을 직접 호출해 409 `CommonException`(`DUPLICATE_EMAIL`, `DUPLICATE_NICKNAME`)을 확인하도록 바꿨다.
  - 서비스의 `@Transactional` 없이 핸들러를 직접 호출하면 `LazyInitializationException`이 나서(운영 코드의 문제가 아니라 테스트 구성 문제), `TransactionTemplate`으로 감쌌다.
- `FlywayMigrationTest`: V3로 `MEMBER`(회원), `ADMIN`(관리자)만 들어갔는지 확인한다.
- `MigrationScriptsTest`: 데이터 전용 파일이 제외되는지 확인한다. 필터가 없어도 도메인 테스트 전체가 깨져서 간접 검증이 되므로, 이 테스트 자체의 검증력은 약한 편이다.
- `CommonExceptionHandlerTest`: 무결성 예외 테스트 2건을 제거했다.

## 검증 결과

- `./gradlew clean build` 성공, `member-api` 테스트 32개, `member-domain` 테스트 107개 모두 성공
- `spotlessApply` 적용, spotless 검사 통과

## 남은 사항

- `SecurityConfig`가 없어 실제 호출은 Spring Security 기본 설정에 막힌다. 다음 단계에서 `/v1/member/join`을 열어야 한다.
- 약관 검증, 약관 동의 이력 저장, 이메일 인증 queue 전송은 TODO이다.
- README에는 아직 반영하지 않았다.
- 다음 작업 순서: Security 기본 설정과 JWT, 로그인(토큰 발급), 로그아웃, 토큰 재발급

---

# Handler의 private 메서드 인라인

> handler에 굳이 private메소드로 찢을필요 없어보여 특히 duplicate email, nickname은 예외 던지는 코드밖에 없는데 굳이 찢을 필요가 없어 복잡해지기만 하고.

## 답변

`duplicateEmail()`, `duplicateNickname()`은 `CommonException`을 생성하는 한 줄뿐이라 메서드로 빼면 읽을 때 오히려 위아래로 오가야 한다는 지적이 맞다고 판단했다.

- 두 메서드를 지우고 `throw new CommonException(...)`을 `validateJoin`의 사전 검증 2곳과 `join`의 `catch` 블록 2곳에 직접 적었다.
- 그 결과 코드명과 메시지 문자열이 이메일과 닉네임 각각 두 번씩 중복된다. 문구를 바꿀 때 두 군데를 같이 고쳐야 하는 비용을 감수하기로 했다.
- `violatedConstraint`는 Hibernate 원인 체인을 순회하는 실제 로직이 있어서 메서드로 남겼다.
- 수정 후 `spotlessApply`와 `./gradlew clean build`가 통과했다. 테스트 코드는 변경하지 않았다.

---

# 동시 요청 대비 catch 유지

> 근데 서비스에서 어차피 먼저 validate를 선제적으로하는데 join에서 catch해서 또 예외처리를 해줘야 할 필요가 있어?

## 답변

사전 검증은 조회 시점에 없다는 것만 보장하고, 조회와 저장 사이의 틈은 막지 못해서 필요하다고 답했다.

- 같은 이메일(또는 닉네임)로 거의 동시에 두 요청이 오면 둘 다 `validateJoin`을 통과하고, 먼저 저장된 쪽만 성공한다. 나중 요청은 DB 유니크 제약에 걸려 `DataIntegrityViolationException`이 난다. 가입 버튼 더블 클릭도 같은 상황을 만든다.
- `catch`를 없애면 이 예외가 일반 `Exception` 핸들러로 가서 500과 error 로그(critical 알람)가 된다. 사용자 실수에 가까운 상황이 오탐 알람이 되고, 사용자에게는 "이미 사용 중인 이메일입니다"가 아니라 서버 오류로 보인다.
- 선택지는 `catch` 유지(약 15줄과 문자열 중복이 비용)와 제거(`join`이 단순해지지만 동시 요청이 500)였고, 알람 오탐을 줄이는 쪽인 유지를 약하게 추천했다.
- 결정: `catch`를 유지한다. 변경 없음.
