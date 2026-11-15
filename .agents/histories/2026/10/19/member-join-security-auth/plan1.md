# 목차

- [개요](#개요)
- [springboot 컨벤션이랑 도메인 구조 읽고 설계안 제시](#springboot-컨벤션이랑-도메인-구조-읽고-설계안-제시)
  - [답변](#답변)
  - [현재 구조에서 확인한 것](#현재-구조에서-확인한-것)
  - [회원가입 API](#회원가입-api)
  - [Spring Security 기본 구조](#spring-security-기본-구조)
  - [JWT](#jwt)
  - [로그인/로그아웃 필터](#로그인로그아웃-필터)
  - [작업 순서 제안](#작업-순서-제안)
  - [확인이 필요한 항목](#확인이-필요한-항목)
- [추가1](#추가1)
- [soft delete된 이메일과 닉네임 정책](#soft-delete된-이메일과-닉네임-정책)
  - [답변](#답변-1)
  - [설계에 미치는 영향](#설계에-미치는-영향)
  - [정해야 할 것](#정해야-할-것)
- [추가2](#추가2)
- [사전검증이 뭔 말인지 모르겠음](#사전검증이-뭔-말인지-모르겠음)
  - [답변](#답변-2)
  - [사전 검증이란](#사전-검증이란)
  - [문제가 되는 부분](#문제가-되는-부분)
  - [동시 요청 처리](#동시-요청-처리)
  - [남은 결정](#남은-결정)
- [회원가입에 중복 체크 valid 로직 구현 계획](#회원가입에-중복-체크-valid-로직-구현-계획)
  - [답변](#답변-3)
  - [구현 계획](#구현-계획)
  - [확인할 것](#확인할-것)
- [Handler 클래스를 Command, Query 두 개로 구성하는 안](#handler-클래스를-command-query-두-개로-구성하는-안)
  - [답변](#답변-4)
  - [근거](#근거)
  - [주의할 점](#주의할-점)
  - [계획 변경](#계획-변경)
- [추가3](#추가3)
- [Request 입력값 제약, 예외 방식, Authority 핸들러 확인](#request-입력값-제약-예외-방식-authority-핸들러-확인)
  - [답변](#답변-5)
  - [Request 입력값 제약](#request-입력값-제약)
  - [MemberDuplicateException을 만든 의도](#memberduplicateexception을-만든-의도)
  - [Authority 핸들러와 리포지토리](#authority-핸들러와-리포지토리)
  - [정해야 할 것](#정해야-할-것-1)
- [추가4](#추가4)
- [추가4 결정 사항과 예외 방침 반영 계획](#추가4-결정-사항과-예외-방침-반영-계획)
  - [답변](#답변-6)
  - [결정 사항](#결정-사항)
  - [예외 방침 이해](#예외-방침-이해)
  - [수정 계획](#수정-계획)
  - [확인 요청](#확인-요청)
- [authority 기본 데이터(DML) 추가](#authority-기본-데이터dml-추가)
  - [답변](#답변-7)
  - [이전 항목](#이전-항목)
  - [authority 기본 데이터](#authority-기본-데이터)
  - [영향 범위](#영향-범위)
  - [추가4 정해야 할 항목](#추가4-정해야-할-항목)

---

# 개요

- 회원가입 API 작성
- spring security 기본 구조 작성
- jwt 관련 코드 작성 
- 로그인, 로그아웃 API도 만들건데 이건 spring security의 필터로 작업할거임

---

# springboot 컨벤션이랑 도메인 구조 읽고 설계안 제시

> springboot 컨벤션이랑 도메인 구조 읽고 설계안 제시해줘

## 답변

`code-convention-springboot.md`, README, `Member` 관련 엔티티와 DTO, 현재 `member-api` 뼈대를 읽고 정리한 설계안이다.

## 현재 구조에서 확인한 것

- `Member`에는 `email`, `password`(암호화된 값), `nickname`, `introduce`, `status`가 있다. 상태 값은 `REGISTER`, `ACTIVATE`, `INACTIVATE`, `SUSPENDED`이다.
- 권한은 `MemberAuthority`로 `Authority`를 참조하고, 약관 동의는 `MemberAgreement`로 `AgreementAnswer`를 참조한다. `AuthorityCode`는 `MEMBER`, `ADMIN`, `MODERATOR`이다.
- 이메일 유니크 제약이 DDL에 있는지, `Authority`에 코드로 조회할 필드가 있는지는 아직 확인하지 않았다. 구현 전에 확인한다.
- `member-api`에는 Security, Validation, Redis 의존성이 있고 JWT 라이브러리는 없다.
- 공통 예외 핸들러는 `AuthenticationException`을 401로 처리한다.
- 컨벤션 문서의 Request 예시는 `@Getter` class와 `@JsonCreator`를 쓰지만, README의 코드 규칙과 현재 `MemberJoinRequest`는 `record`이다. 프로젝트 현행(record + `@Builder(toBuilder = true)`)을 따른다.
- `MemberCommandController`의 `Join` 메서드는 `join`으로 고치고 `@PostMapping`을 붙인다.

## 회원가입 API

`POST /v1/members`. 컨벤션대로 Controller는 Service 함수를 하나만 호출한다.

### MemberJoinRequest

record, `@Valid` 검증.

- `email`: `@NotBlank @Email @Size(max=255)`
- `password`: `@NotBlank`, 길이와 패턴 검증
- `nickname`: `@NotBlank @Size`
- `introduce`: 선택
- `agreementAnswerIds`: 선택한 약관 항목 ID 목록

### MemberJoinResponse

`id`, `email`, `nickname`, `status`. 비밀번호는 포함하지 않는다.

### 흐름

`MemberCommandService.join` (`@Transactional`)

1. `MemberValidateHandler`: 이메일과 닉네임 중복 검증. 위반 시 `CommonException` 409.
2. `AgreementValidateHandler`: 필수 약관 항목이 모두 포함됐는지, 존재하는 항목인지 검증. 약관 도메인 Handler를 Service가 호출하는 방식이라 컨벤션에 맞다.
3. `MemberJoinHandler`: 비밀번호 해싱 후 `Member` 생성. 기본 권한 `MEMBER`와 약관 동의 이력을 `addAuthority`, `addAgreement`로 등록하고 저장한 뒤 `MemberDto`로 반환한다.
4. Service에서 `MemberJoinResponse.from(dto)`로 변환한다.

### 결정이 필요한 점

- 가입 직후 상태를 `REGISTER`(이메일 인증 대기)로 할지 `ACTIVATE`로 할지 정해야 한다. 이메일 인증을 이번 범위에 넣지 않으면 `ACTIVATE`를 제안한다.
- Authority 조회는 `AuthorityCommandHandler` 같은 권한 도메인 Handler를 거친다. Member Handler가 `AuthorityRepository`를 직접 쓰지 않는 구조이다.

## Spring Security 기본 구조

- `SecurityConfig`
  - stateless 세션, CSRF 비활성화
  - `POST /v1/members`, 로그인, 토큰 재발급은 `permitAll`, 나머지는 인증 필요
  - 필터 등록 순서: `JwtAuthenticationFilter`(요청 인증), `LoginFilter`(로그인), `LogoutFilter`(로그아웃)
- `PasswordEncoder`: `BCryptPasswordEncoder` 빈
- `MemberUserDetailsService`: 이메일로 회원을 조회해 `UserDetails`(권한은 `ROLE_{AuthorityCode}`)를 만든다. 이 서비스는 Security 계층이라 컨벤션의 Service/Handler 체인에서 따로 다룬다.
- 인증 실패는 `AuthenticationEntryPoint`로 401 `CommonResponse` JSON을 내려준다. 필터 단계 예외는 `@RestControllerAdvice`에 닿지 않으므로 별도로 처리한다. 인가 실패는 `AccessDeniedHandler`로 403을 내려준다.
- 패키지는 `dev.choimory.member.api.common.security` 아래에 `config`, `filter`, `jwt`, `handler`로 나누는 안을 제안한다.

## JWT

- 라이브러리: `io.jsonwebtoken:jjwt` 0.12.x
- 토큰
  - Access Token: 짧은 만료(예: 30분), 클레임은 `sub`=회원 id, `authorities`
  - Refresh Token: 긴 만료(예: 14일), Redis에 저장해 로그아웃 시 폐기 가능
- 서명: HS256, 시크릿은 환경변수로 받는다(`jwt.secret`, `jwt.access-expiration`, `jwt.refresh-expiration`). `@ConfigurationProperties` 객체로 묶는다.
- 구성 클래스: `JwtProvider`(생성, 검증, 파싱), `JwtProperties`
- 로그아웃은 Redis에서 Refresh Token을 삭제한다. 남은 Access Token은 블랙리스트에 등록할 수 있다.

## 로그인/로그아웃 필터

- `LoginFilter`: `UsernamePasswordAuthenticationFilter`를 상속해 `POST /v1/auth/login`을 처리한다.
  - 요청: `{email, password}` JSON
  - 성공: Access/Refresh Token 발급, Refresh Token을 Redis에 저장, `CommonResponse`로 응답
  - 실패: `AuthenticationException`으로 401
- `LogoutFilter`: `POST /v1/auth/logout`을 처리한다. Refresh Token을 폐기하고, 필요하면 Access Token을 블랙리스트에 올린다.
- `JwtAuthenticationFilter`: `OncePerRequestFilter`로 `Authorization: Bearer`를 검증하고 `SecurityContext`에 인증을 채운다.
- 토큰 재발급 `POST /v1/auth/refresh`는 필터가 아닌 일반 Controller로 둘지 정해야 한다.

## 작업 순서 제안

1. 회원가입: Request/Response 필드, 중복·약관 검증, 저장
2. Security 기본 설정: `SecurityConfig`, `PasswordEncoder`, EntryPoint/AccessDeniedHandler
3. JWT: `JwtProvider`, `JwtProperties`, `JwtAuthenticationFilter`
4. 로그인/로그아웃 필터와 Redis 연동
5. 각 단계마다 테스트(규칙상 시작 전에 확인) 후 README 반영

## 확인이 필요한 항목

1. 가입 직후 `member.status`: `ACTIVATE`와 `REGISTER`(이메일 인증 도입) 중 무엇으로 할까?
2. 약관 동의를 가입 요청에 포함할지, 필수 약관 검증을 넣을지?
3. Refresh Token 저장소로 Redis를 쓰는 안에 동의하는지? Redis 설정은 아직 `member-api`에 없다.
4. 토큰 재발급 API는 이번 범위에 포함할지?
5. 닉네임 중복을 막을지, 이메일만 막을지? DDL의 유니크 제약은 확인 후 보고한다.

---

# 추가1

- 일단은 ACTIVATE로 진행. 이후 이메일 인증 추가할때 REGISTER를 거쳐 ACTIVATE로 올리는 형태로 할거임 
- 회원가입 API의 비즈니스 로직 중 이메일 인증 queue로 보내는 부분이랑 약관쪽 TODO로 두고 지금은 일단 회원가입만 진행
- redis 사용할거임
- 토큰 재발급 API도 작업할거임. 회원가입 -> security, jwt -> 토큰 발급, 재발급 순으로 할거임
- 닉네임 중복 막도록 유니크처리 되어있을거임 docs/db 확인 

---

# soft delete된 이메일과 닉네임 정책

> ㄴㄴ soft delete한 이메일과 닉네임은 이후 배치로 hard delete 하기 전까진 사용 불가능

## 답변

정책이 DDL과 일치한다. 유니크 제약이 soft delete 행까지 포함하므로, 배치로 hard delete하기 전에는 DB가 재사용을 막는다.

## 설계에 미치는 영향

- 사전 중복 검증에서 삭제된 행도 중복으로 판정해야 한다. 그렇지 않으면 `@SQLRestriction("deleted_at IS NULL")` 때문에 `existsByEmail`이 삭제된 행을 못 본다. 검증을 통과해도 저장 시점에 유니크 위반(500)이 난다.
- 삭제 행까지 보는 네이티브 쿼리로 검증하는 안을 제안한다.
  - 예: `select exists(select 1 from member where email = :email)`. 이 쿼리는 `@SQLRestriction`이 적용되지 않는다.
  - `MemberRepository`에 `existsByEmailIncludingDeleted`, `existsByNicknameIncludingDeleted` 두 메서드를 둔다. 메서드명과 쿼리 방식은 구현 시 확정한다.
- 동시 가입 요청은 사전 검증을 둘 다 통과할 수 있다. 이 경우 DB 유니크가 최종으로 막고, `DataIntegrityViolationException`을 409로 변환하는 처리를 두는 안을 제안한다.
- 이메일과 닉네임 중 어느 쪽이 중복인지 구분해 메시지를 내려주면 클라이언트가 어느 필드를 고칠지 알 수 있다. 제약명(`uk_member_email`, `uk_member_nickname`)으로 구분하는 안이다.

## 정해야 할 것

1. 사전 검증에 네이티브 쿼리로 삭제 행까지 포함하는 방식에 동의하는지?
2. 동시 요청 대비로 `DataIntegrityViolationException`을 409로 변환하는 처리도 같이 넣을지? 이미 있는 `CommonExceptionHandler`를 건드리는 변경이다.

---

# 추가2

- 사전검증이 뭔 말인지 모르겠음
- 동시요청 대비 처리은 얘기한대로 진행해줘

---

# 사전검증이 뭔 말인지 모르겠음

> 사전검증이 뭔 말인지 모르겠음

## 답변

사전 검증은 회원을 저장하기 전에 "이 이메일/닉네임을 이미 쓰는 사람이 있나?"를 먼저 조회해서 확인하는 것이다. 동시 요청 대비 처리는 이야기한 대로 진행하기로 확정했다.

## 사전 검증이란

가입 흐름은 다음과 같다.

1. 요청이 들어온다. (이메일 `a@a.com`)
2. 사전 검증: DB에 `a@a.com`이 이미 있는지 조회한다. 있으면 "이미 사용 중인 이메일입니다"(409)를 내려주고 끝낸다.
3. 없으면 비밀번호를 해싱하고 저장한다.

사전 검증이 없어도 DB의 유니크 제약이 중복 저장을 막는다. 하지만 그때는 DB 예외가 나서 어느 값이 중복인지 알려주는 깔끔한 응답을 만들기 어렵다. 그래서 먼저 조회해서 친절한 에러를 내려주려는 것이다.

## 문제가 되는 부분

탈퇴한 회원(soft delete)의 이메일은 hard delete 배치 전까지 쓸 수 없는데, 일반 조회는 삭제된 행을 못 본다.

- 탈퇴한 회원의 이메일 `a@a.com`으로 새로 가입한다.
- 일반 조회: "없음"으로 나와 검증을 통과한다.
- 저장하면 DB 유니크 제약에 걸려 에러가 난다. 이때 사용자에게는 깔끔한 409가 아니라 500처럼 보인다.

그래서 삭제된 행까지 조회하는 쿼리로 사전 검증을 하자는 제안이다.

## 동시 요청 처리

확정. 같은 이메일로 거의 동시에 두 요청이 오면 둘 다 사전 검증을 통과할 수 있다. 이때 DB 유니크 제약이 두 번째 저장을 막고, 그 예외(`DataIntegrityViolationException`)를 409로 바꿔서 내려준다. 제약명(`uk_member_email`, `uk_member_nickname`)으로 이메일 중복인지 닉네임 중복인지 구분한다. 이 변환은 `CommonExceptionHandler`에 추가한다.

## 남은 결정

사전 검증을 삭제된 행까지 포함해서 조회하는 방식으로 갈지? (제안은 "예")

---

# 회원가입에 중복 체크 valid 로직 구현 계획

> ㅇㅇ 회원가입에 중복 체크 valid 로직도 넣어줘

## 답변

사전 검증은 삭제된 행까지 포함해서 조회하고, 동시 요청 대비로 `DataIntegrityViolationException`을 409로 변환하는 것으로 확정했다. 프로젝트 규칙상 코드 작성 전에 계획을 설명하고 시작 여부를 확인한다. 현재 `calendar.md` 상태는 PLAN이다.

## 구현 계획

1. Repository: `MemberRepository`에 삭제 행까지 보는 네이티브 쿼리 두 개를 추가한다.
   - `existsByEmailIncludingDeleted(email)`
   - `existsByNicknameIncludingDeleted(nickname)`
2. Handler: `MemberValidateHandler`를 새로 만든다. `validateJoin(request)`가 이메일과 닉네임 중복을 검사하고, 중복이면 `CommonException`(409)을 던진다. 이메일과 닉네임을 구분한 메시지를 쓴다.
3. Service: `MemberCommandService.join`이 위 Handler를 호출한다. 이어서 `MemberJoinHandler`를 호출해 비밀번호 해싱 후 저장하고, `MemberJoinResponse`를 반환한다.
4. Request/Response: `MemberJoinRequest`에 `email`, `password`, `nickname`, `introduce`와 검증 애노테이션을 넣는다. `MemberJoinResponse`에는 `id`, `email`, `nickname`, `status`를 넣는다. 약관과 이메일 인증 queue는 `// TODO`로 둔다.
5. Controller: `Join`을 `join`으로 고치고 `@PostMapping`, `@Valid @RequestBody`를 붙인다.
6. 동시 요청 처리: `CommonExceptionHandler`에 `DataIntegrityViolationException` 처리를 추가한다. 제약명(`uk_member_email`, `uk_member_nickname`)으로 이메일/닉네임 중복을 구분해 409를 내려준다. 그 외 무결성 위반은 기존처럼 500으로 둔다.
7. 의존성: 비밀번호 해싱용 `PasswordEncoder` 빈이 필요하다. 최소한의 `BCryptPasswordEncoder` 빈만 `config`에 추가한다. Security 본 설정은 다음 단계에서 한다.
8. 테스트: Docker(Testcontainers)가 필요하다. 중복 이메일, 중복 닉네임, 삭제된 회원의 이메일·닉네임 재사용 불가, 동시 요청 시 409 변환, 정상 가입을 확인한다.
9. 문서: 끝나면 결과를 설명하고, `plan1.md`, `task1.md` 기록과 `calendar.md`의 PLAN을 TASK로 바꾸는 것은 지시를 받은 뒤에 한다.

## 확인할 것

- 이 계획으로 코드 작성을 시작해도 되는지? 시작하면 `calendar.md`를 `TASK`로 바꾼다.
- 회원 저장까지 같이 구현하는 범위(3번)로 괜찮은지, 아니면 중복 검증만 먼저 할지?
- 테스트 실행(Docker 필요)도 같이 진행해도 되는지?

---

# Handler 클래스를 Command, Query 두 개로 구성하는 안

> MemberValidatedHander, MemberJoinHandler 이렇게 기능단위로 핸들러 클래스 만드는거 말고 MemberCommandHandler, MemberQueryHandler 이렇게 두개만 만드려 하는데 이건 별로야?

## 답변

별로 아니고, 이 프로젝트에는 두 클래스 구조가 더 잘 맞는다.

## 근거

- README와 현재 뼈대와 일치한다. README에 클래스 이름이 `{Domain}{Command|Query}{Layer}`로 정해져 있고, `MemberCommandHandler`와 `MemberQueryHandler`가 이미 있다. `MemberValidateHandler`, `MemberJoinHandler`를 따로 만들면 이 규칙에서 벗어난다.
- 컨벤션 문서는 클래스 분리를 강제하지 않는다. 규칙은 "Handler 함수 하나는 하나의 세부 로직만 담당한다"이다. 클래스를 기능별로 나누라는 말은 없다. 문서의 `PostValidateHandler`, `PostCreateHandler` 분리는 예시일 뿐이다.
- query Handler로 중복 검증을 할 수 없다. README에 따르면 query는 Elasticsearch 조회로 구현할 예정이고, 중복 검증은 JPA로 DB를 직접 봐야 한다(삭제 행 포함 네이티브 쿼리). 그래서 검증은 `MemberCommandHandler`에 둔다.

## 주의할 점

- 메서드 단위 책임을 지킨다. `validateJoin`, `join`처럼 메서드 하나가 로직 하나만 담당하게 한다.
- 도메인이 커지면 `MemberCommandHandler`가 비대해질 수 있다. 지금은 가입, 프로필 수정, 비밀번호 변경 정도라 괜찮고, 커지면 그때 나눈다.
- 다른 도메인 로직(권한 부여 등)은 Member Handler가 `AuthorityRepository`를 직접 쓰지 않고, Service가 `AuthorityCommandHandler`를 호출한다. 이 부분은 두 클래스 구조에서도 같다.

## 계획 변경

- 구현 계획 2번의 `MemberValidateHandler`는 `MemberCommandHandler.validateJoin(request)`로 바꾼다.
- 구현 계획 3번의 `MemberJoinHandler`는 `MemberCommandHandler.join(request)`로 바꾼다.

---

# 추가3

- ~~Request 객체에 Spring valid로 입력값 제약을 건다~~
  - ~~이메일 형식~~
  - ~~닉네임 특문제외, 한글 영문 숫자만 3~15글자~~
  - ~~비밀번호 영문 숫자 특문 포함해서 최소 8글자~~
  - 아 이건 되어 있구나 생략
- 예외던지기 할때 별도 예외는 만들지 않고 CommonException을 이용해서 던질 생각이였는데 MemberDuplicateException이라는 CommonException을 상속한 커스텀 예외를 추가했고 이걸 예외 핸들러에서 추가작성해서 받게 했는데 이거 의도가 궁금
- Authority에 핸들러랑 리포지토리는 회원가입할때 권한 저장이 필요해서 작성한건가?

---

# Request 입력값 제약, 예외 방식, Authority 핸들러 확인

> - Request 객체에 Spring valid로 입력값 제약을 건다
>   - 이메일 형식
>   - 닉네임 특문제외, 한글 영문 숫자만 3~15글자
>   - 비밀번호 영문 숫자 특문 포함해서 최소 8글자
> - 예외던지기 할때 별도 예외는 만들지 않고 CommonException을 이용해서 던질 생각이였는데 MemberDuplicateException이라는 CommonException을 상속한 커스텀 예외를 추가했고 이걸 예외 핸들러에서 추가작성해서 받게 했는데 이거 의도가 궁금
> - Authority에 핸들러랑 리포지토리는 회원가입할때 권한 필요해서 작성한건가?

## 답변

"추가3"의 세 가지에 대한 답변이다. 이 시점에는 코드를 수정하지 않았다.

## Request 입력값 제약

현재 `MemberJoinRequest`는 임의로 정한 값이라 요청 내용과 다르다.

| 필드 | 현재 | 요청 내용 |
|---|---|---|
| 이메일 | `@NotBlank @Email @Size(max=255)` | 이메일 형식 (유지) |
| 닉네임 | 2~20자, 문자 제한 없음 | 특수문자 제외, 한글, 영문, 숫자만 3~15자 |
| 비밀번호 | 8~64자, 영문, 숫자, 특수문자 모두 포함 | 영문, 숫자, 특수문자를 포함해 최소 8자 |

수정 안은 다음과 같다.

- 닉네임: `@Pattern(regexp = "^[가-힣A-Za-z0-9]{3,15}$")` 하나로 문자와 길이를 함께 검증한다.
- 비밀번호: 최소 8자만 요청했는데 64자 상한을 넣어 둔 상태이다. 상한은 `BCrypt`가 72바이트까지만 반영하기 때문에 넣었다. 상한을 유지할지, 유지한다면 64자를 그대로 쓸지 정해야 한다.
- 이메일: `@Email`은 `a@a`처럼 도메인 점이 없는 값도 통과시킨다. 더 엄격하게 하려면 `@Pattern`을 추가해야 하는데, 필요한지 정해야 한다.
- 자기소개 500자 제한도 임의로 넣은 값이다. 유지할지 정해야 한다.

## MemberDuplicateException을 만든 의도

`CommonException`을 직접 던질 생각이었는데 별도 예외를 임의로 만들었다. 먼저 물어봤어야 했다. 만든 이유는 두 가지이다.

- 사전 검증(`validateJoin`)과 동시 요청 변환(`CommonExceptionHandler`)이 같은 409 응답(코드명과 메시지)을 내려야 해서, 문자열이 두 곳에 중복되는 것을 막으려고 한곳에 모았다.
- 유니크 제약명 상수(`uk_member_email`, `uk_member_nickname`)도 같이 두었다.

단점도 있다. 공통 패키지(`common.exception`)가 member 패키지를 import하게 되어 의존 방향이 어색하다. 이 예외는 꼭 필요하지 않다.

### 대안 A

커스텀 예외를 없애고 Handler가 `CommonException`을 직접 던진다. 동시 요청 변환은 `CommonExceptionHandler`에 두는 대신 `MemberCommandHandler.join`에서 `saveAndFlush`의 `DataIntegrityViolationException`을 잡아 같은 `CommonException`(409)으로 바꿔 던진다. 중복 문구는 `MemberCommandHandler` 안의 private 메서드 두 개(`duplicateEmail()`, `duplicateNickname()`)로 모은다. 그러면 `CommonExceptionHandler`를 건드리지 않고 의존 방향 문제도 없다. 이전에 확정한 "동시 요청은 `CommonExceptionHandler`에 추가" 방침과는 달라지므로 동의가 필요하다.

### 대안 B

지금 구조를 유지한다.

추천은 A이다. 원래 생각한 방식에 가깝고 변경 범위도 member 도메인 안에 머문다.

## Authority 핸들러와 리포지토리

맞다. 가입 시 기본 권한(`MEMBER`)이 필요해서 추가했다. `AuthorityCommandHandler`와 `AuthorityRepository` 클래스는 원래 뼈대에 있었고, 추가한 것은 `findByCode` 메서드 두 개(Repository, Handler)뿐이다. 컨벤션상 Handler는 자기 도메인의 Repository만 쓸 수 있어서, `MemberCommandHandler`가 `AuthorityRepository`를 직접 쓰지 않고 `MemberCommandService`가 `AuthorityCommandHandler`를 호출해 권한을 받아 오는 구조로 했다. 이 때문에 `MemberCommandHandler.join`이 `EntityManager.getReference`를 쓰는 예외가 생겼고, `task1.md`에도 적어 두었다.

## 정해야 할 것

1. 비밀번호 상한(64자)을 둘지, 이메일을 엄격하게 할지, 자기소개 500자 제한을 유지할지
2. 예외 방식을 A로 갈지 B로 유지할지

---

# 추가4

- 이메일 제약에 패턴 추가 (a@b 방지)
- 비밀번호 제약은 너의 방식 유지
- 닉네임은 내가 제안한 제약대로
- 공통 예외를 사용하라는 이유는 2가지임
  - 추후 예외 핸들러에 로그 달고 에외상황이 critical한지 아닌지 알람도 보내서 모니터링 해야하는데, 매번 예외추가하면 계속 늘어나서 손 많이가고 복잡함. 
  - 공통예외는 기본적으로 critical한 상황이 아니기 때문에 비즈니스적인 예외는 공통예외에 메세지 넣어서 던지면 처리 가능.
- DML에 authority, menu 기본 데이터 추가
  - authority: MEMBER(회원), ADMIN(관리자)
  - menu: 이건 일단 보류. 메뉴 구체화될때 추가

---

# 추가4 결정 사항과 예외 방침 반영 계획

> - 이메일 제약에 패턴 추가 (a@b 방지)
> - 비밀번호 제약은 너의 방식 유지
> - 닉네임은 내가 제안한 제약대로
> - 공통 예외를 사용하라는 이유는 2가지임
>   - 추후 예외 핸들러에 로그 달고 에외상황이 critical한지 아닌지 알람도 보내서 모니터링 해야하는데, 매번 예외추가하면 계속 늘어나서 손 많이가고 복잡함. 
>   - 공통예외는 기본적으로 critical한 상황이 아니기 때문에 비즈니스적인 예외는 공통예외에 메세지 넣어서 던지면 처리 가능.

## 답변

"추가4"의 결정 사항을 정리하고 예외 방침을 반영한 수정 계획을 세웠다. 이 시점에는 코드를 수정하지 않았다.

## 결정 사항

| 항목 | 결정 |
|---|---|
| 이메일 | `a@b`처럼 점이 없는 도메인을 막는 패턴 추가 |
| 비밀번호 | 제안한 방식 유지 (8~64자, 영문·숫자·특수문자 모두 포함) |
| 닉네임 | 제안한 대로 (특수문자 제외, 한글·영문·숫자만 3~15자) |
| 예외 | 별도 예외를 만들지 않고 `CommonException`에 메시지를 담아 던짐 |

## 예외 방침 이해

- 비즈니스 예외는 critical하지 않은 상황이라 `CommonException`으로 던진다. 핸들러에서 로그와 알람을 한 곳에서 일관되게 처리하고, 예외 클래스가 늘어나 복잡해지는 것을 막으려는 의도로 이해했다.
- 반대로 `CommonException`이 아닌 예외는 모두 일반 `Exception` 핸들러로 가서 error 로그와 500이 되므로 critical로 분류된다.
- 이 기준이 지금 구조에 잘 맞는다. 중복 가입은 non-critical(`CommonException` 409)이고, `MEMBER` 권한 행이 없어 나는 `IllegalStateException`은 critical(500)이다.
- `MemberDuplicateException`은 이 방침과 맞지 않아 제거한다.

## 수정 계획

대안 A 기준이다.

1. `MemberJoinRequest`
   - 이메일: `@Email`에 `@Pattern` 추가 (예: `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`). 영문 도메인만 허용하고 한글 도메인 등은 막힌다.
   - 닉네임: `@Pattern(regexp = "^[가-힣A-Za-z0-9]{3,15}$")`로 교체하고 `@Size`는 뺀다. 자기소개 500자와 비밀번호 규칙은 그대로 둔다.
2. `MemberDuplicateException` 삭제
3. `MemberCommandHandler`
   - 중복 시 `new CommonException(HttpStatus.CONFLICT, 409, "DUPLICATE_EMAIL" 또는 "DUPLICATE_NICKNAME", 메시지)`를 던진다. 문구는 클래스 안의 private 메서드 두 개로 모은다.
   - `join`에서 `saveAndFlush`가 던지는 `DataIntegrityViolationException`을 잡아, 위반한 제약명으로 같은 `CommonException`(409)으로 바꿔 던진다. 알 수 없는 제약 위반은 잡지 않고 그대로 던져 critical(500)로 간다.
   - 제약명을 찾는 로직(Hibernate 원인 체인과 메시지 폴백)도 이 클래스로 옮긴다.
4. `CommonExceptionHandler`: 추가했던 `DataIntegrityViolationException` 처리를 제거해 원래대로 되돌린다. member 패키지 import도 없어진다.
5. 테스트
   - `CommonExceptionHandlerTest`에 추가한 무결성 예외 테스트 2건을 제거한다.
   - `MemberJoinTest`에서는 변환 테스트를 "사전 검증을 건너뛰고 `MemberCommandHandler.join`을 직접 호출했을 때 `CommonException`(409, `DUPLICATE_EMAIL`/`DUPLICATE_NICKNAME`)이 나오는지"로 바꾼다.
   - 닉네임 허용·불허 케이스(특수문자, 2자와 16자 경계, 한글), 이메일 `a@b` 거부 케이스를 추가한다.
6. 수정 후 `spotlessApply`와 `clean build`로 확인하고 변경 내용을 설명한다. `task`와 `plan` 문서는 요청할 때만 작성한다.

## 확인 요청

- 이 계획으로 수정을 시작해도 되는지
- 이메일 패턴이 위 정규식으로 괜찮은지

---

# authority 기본 데이터(DML) 추가

> - DML에 authority, menu 기본 데이터 추가
>   - authority: MEMBER(회원), ADMIN(관리자)
>   - menu: 이건 일단 보류. 메뉴 구체화될때 추가

## 답변

"추가4"에 새로 생긴 DML 항목에 대한 답변이다. authority에 `MEMBER`(회원)와 `ADMIN`(관리자)을 넣고, menu는 메뉴가 구체화될 때 추가하는 것으로 보류한다고 이해했다. 이 시점에는 코드를 수정하지 않았다.

## 이전 항목

이메일 패턴 추가, 비밀번호 유지, 닉네임 제약, `MemberDuplicateException` 제거 후 `CommonException` 사용, 동시 요청 변환을 `MemberCommandHandler`로 이동하는 계획은 위 섹션의 내용 그대로이다.

## authority 기본 데이터

`docs/db/v1/choimory-dev-member-api-dml-v1.sql`은 비어 있고 "참고용이며 앱과 테스트에서 사용하지 않는다"고 되어 있다. 앱에 실제로 적용되려면 Flyway 마이그레이션 `docs/flyway/v1/V3__init_authority.sql`로 넣어야 한다. 현재 V1과 V2가 있다.

### V3 내용 제안

```sql
insert into authority (id, name, code, created_at)
values ('<UUID v7>', '회원', 'MEMBER', now()),
       ('<UUID v7>', '관리자', 'ADMIN', now())
on conflict (code) do nothing;
```

- id는 서버가 UUID v7으로 채번하는 값이라, SQL에는 미리 만든 UUID v7 리터럴을 고정으로 넣는 방식을 제안한다. 환경마다 id가 같아진다.
- `on conflict (code) do nothing`은 `uk_authority_code` 때문에 재실행해도 안전하게 하려는 것이다.
- `AuthorityCode` enum에 있는 `MODERATOR`는 데이터에 넣지 않는다. enum 값은 그대로 둔다.

## 영향 범위

기존 테스트가 깨질 수 있다.

- `member-domain` 테스트는 `docs/flyway`의 `V*.sql`을 모두 컨테이너에 적용하는데, V3로 `MEMBER`, `ADMIN` 행이 들어가면 같은 코드를 직접 insert하는 테스트가 유니크 위반으로 실패한다.
- 해당 테스트: `AuthorityEntitiesTest`(ADMIN 저장), `DdlScriptTest`(`authorityCodeIsUnique`, `memberAuthorityIsUnique` 등).
- 대응은 두 가지 중 하나이다.
  - (1) 도메인 테스트가 V3 같은 데이터 전용 마이그레이션을 건너뛰게 한다. 스키마 검증이 목적이므로 이 방식을 추천한다.
  - (2) 영향받는 테스트마다 setup에서 authority를 비운다.
- `member-api`의 `MemberJoinTest`는 지금 직접 `MEMBER`를 insert하는데, V3가 있으면 그 insert를 빼고 seed를 쓰도록 바꾼다. `truncate`에서도 authority를 제외한다.
- 새 테스트로 "앱 기동 후 `MEMBER`, `ADMIN` 권한이 존재한다"를 `FlywayMigrationTest`에 추가한다.
- 참고용 DML 파일 `docs/db/v1/choimory-dev-member-api-dml-v1.sql`에도 같은 insert를 반영할지 정해야 한다.

## 추가4 정해야 할 항목

1. 도메인 테스트 대응을 (1) 데이터 전용 마이그레이션 제외로 갈지 (2) 테스트별 정리로 갈지
2. UUID v7 리터럴을 미리 만들어 고정으로 넣어도 되는지 (id를 서버 채번 대신 SQL에 두는 첫 사례이다)
3. 참고용 DML 파일도 같이 갱신할지
4. 이메일 정규식이 괜찮은지
5. 위 수정 계획(Request 제약, 예외 정리, V3)을 한 번에 진행해도 되는지

--- 

# 추가5

- dml이랑 flyway에 모두 dml을 반영해야함. 
- uuid 고정해도됨
- 이메일 정규식 괜찮음
- 도메인테스트는 1로 해줘