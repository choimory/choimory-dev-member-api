# 목차

- [개요](#개요)
- [코드 컨벤션 수정 구현](#코드-컨벤션-수정-구현)
- [수정 상세](#수정-상세)
- [검증](#검증)
- [남은 후속 범위](#남은-후속-범위)

---

# 개요

- Step 2 Java 이관 코드에 대해 `plan4.md`의 코드 컨벤션 수정 계획을 일부 구현했다.
- 주요 범위는 요청 바인딩 정리, Response와 CommonResponse 정적 팩토리 추가, 생성자 직접 호출 제거, `MemberDocumentDto` 1:1 매칭, 빈 Request/Response 정리다.
- 테스트 보강은 범위가 커서 별도 후속 작업으로 남겼다.

---

# 코드 컨벤션 수정 구현

> 구현시작

## 답변

`plan4.md` 기준으로 컨벤션 정리 구현을 진행했다.

## 구현 범위

- Controller 요청 바인딩 정리
- Builder와 정적 팩토리 기반 객체 생성 정리
- `CommonResponse` 응답 정책 정리
- `MemberDocumentDto` 1:1 매칭
- 빈 Request/Response 객체 정리
- 기존 context test 유지 확인

---

# 수정 상세

## Controller 요청 바인딩

`MemberCommandController`의 요청 바인딩을 정리했다.

- `sign-up`
  - `CreateMemberRequest`가 `MultipartFile profile`을 포함하므로 `@ModelAttribute`를 적용했다.
- `verify`
  - JSON 요청으로 보고 `@RequestBody`를 적용했다.

```java
public CommonResponse<CreateMemberResponse> signUp(
        @Valid @ModelAttribute CreateMemberRequest payload
)
```

```java
public CommonResponse<VerifyMemberResponse> verify(
        @Valid @RequestBody VerifyMemberRequest payload
)
```

## CommonResponse 정적 팩토리

`CommonResponse`에 응답 정책 정적 팩토리를 추가했다.

- `of(HttpStatus status, String message, T data)`
- `ok(T data)`
- `created(T data)`
- `error(HttpStatus status, String message)`
- `error(HttpStatus status, String message, T data)`

내부 생성은 builder 기반으로 처리하도록 했다.

## Response 정적 팩토리

Response 객체에 정적 팩토리를 추가했다.

- `CreateMemberResponse.of(uuid, verifyCode)`
- `VerifyMemberResponse.of(accessToken, refreshToken)`
- `LoginResponse.of(accessToken, refreshToken)`

Service, Handler, Security Handler의 직접 생성자 호출을 정적 팩토리 호출로 변경했다.

## TokenDetails 정적 팩토리

`TokenDetails.from(MemberDocumentDto member)`를 추가했다.

기존 `of(member)`는 `from(member)`으로 위임하도록 했다.

## MemberDocumentDto 1:1 매칭

`MemberDocumentDto`를 `MemberDocument`와 1:1로 매칭되도록 확장했다.

추가한 필드:

- `id`
- `nickname`
- `password`
- `introduce`
- `commonDate`

기존 `email` 필드는 유지했다.

`from(MemberDocument)`에서는 builder를 사용해 전체 필드를 매핑하도록 변경했다.

## 빈 Request/Response 정리

필드가 아직 없는 query 요청/응답 객체를 `final class`로 변경했다.

- `FindMemberRequest`
- `FindMemberResponse`

## 예외/보안 응답 정리

다음 클래스에서 `new CommonResponse<>(...)` 직접 호출을 제거했다.

- `CommonExceptionHandler`
- `LoginFailureHandler`
- `LoginSuccessHandler`

대신 `CommonResponse.error(...)`, `CommonResponse.ok(...)`, builder 기반 생성을 사용했다.

---

# 검증

## compileJava

```text
./gradlew compileJava
BUILD SUCCESSFUL
```

## compileTestJava

```text
./gradlew compileTestJava
BUILD SUCCESSFUL
```

## test

```text
./gradlew test
BUILD SUCCESSFUL
```

---

# 남은 후속 범위

- access token, refresh token 실제 생성 로직 구현
- 로그인 성공 시 Redis 토큰 등록 구현
- 회원가입 인증 메일 Kafka 발송 구현
- 함수 단위 테스트 작성
- API 단위 통합 테스트 작성
