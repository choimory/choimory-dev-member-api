# 목차

- [개요](#개요)
- [회원가입 API 및 중복 체크 구현](#회원가입-api-및-중복-체크-구현)
  - [답변](#답변)
  - [작업 내용](#작업-내용)
  - [테스트](#테스트)
  - [검증 결과](#검증-결과)
  - [남은 사항](#남은-사항)

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
