# 목차

- [개요](#개요)
- [코드 컨벤션 검사](#코드-컨벤션-검사)
- [Builder 활용 부족](#builder-활용-부족)
- [코드 컨벤션 수정 계획](#코드-컨벤션-수정-계획)
- [수정 우선순위](#수정-우선순위)

---

# 개요

- Step 2 Java 이관 코드가 `.agents/rules/back-end`의 Spring Boot 코드 컨벤션, 주석 규칙, 테스트 규칙을 만족하는지 검토했다.
- 컴파일과 테스트는 통과했지만, rules 기준으로는 일부 컨벤션 불일치가 남아 있다.
- 이 문서는 이후 컨벤션 정리 작업을 위한 계획과 점검 결과를 기록한다.

---

# 코드 컨벤션 검사

> rules보고 코드 컨벤션대로 이관했는지 검사 한번 해보자

## 답변

검사 결과, 전체적으로 Java 이관은 되었고 컴파일과 테스트는 통과하지만 “완전히 컨벤션대로 이관”된 상태는 아니다.

기존 Kotlin 원본을 보존하려다 남긴 부분과 Java 이관 과정에서 컨벤션에 더 맞춰야 하는 부분이 섞여 있다.

## 확인한 규칙

- `.agents/rules/back-end/code-convention-springboot.md`
- `.agents/rules/back-end/comment.md`
- `.agents/rules/back-end/test.md`

## 정상적으로 맞는 부분

- DTO, Request, Response 대부분은 `final`, `private final`, `@Getter`, `@Builder` 기반 불변 객체 형태다.
- Entity에는 DTO 변환 메소드를 작성하지 않았다.
- Entity에서 DTO로의 변환은 DTO 쪽에 작성되어 있다.
- Controller는 Service 함수 하나만 호출한다.
- Service가 트랜잭션을 담당한다.
- Handler가 Repository 호출 후 DTO를 반환한다.
- 클래스, 필드, 주요 함수에는 한글 Doc Comment 또는 필드 주석이 대부분 작성되어 있다.

## 불일치 1. Controller 요청 바인딩

`MemberCommandController`의 요청 파라미터가 `@Valid`만 사용하고 있다.

```java
public CommonResponse<CreateMemberResponse> signUp(@Valid CreateMemberRequest payload)
```

```java
public CommonResponse<VerifyMemberResponse> verify(@Valid VerifyMemberRequest payload)
```

rules 예시는 `@Valid @RequestBody Request` 형태다.

현재 상태에서는 JSON body가 아니라 model attribute 또는 form 바인딩처럼 동작할 수 있다.

다만 `CreateMemberRequest`에는 `MultipartFile profile`이 있으므로 무조건 `@RequestBody`를 붙이는 것이 답은 아닐 수 있다.

### 정리 방향

- `verify`는 JSON 요청으로 보고 `@RequestBody`를 붙이는 방향이 자연스럽다.
- `sign-up`은 multipart API로 유지할 경우 `@ModelAttribute`를 사용하는 방향이 자연스럽다.
- `sign-up`을 JSON API로 바꿀 경우 `profile` 필드를 제거하거나 별도 업로드 API로 분리한 뒤 `@RequestBody`를 사용하는 방향이 좋다.

## 불일치 2. MemberDocumentDto 1:1 매칭

`MemberDocument`는 다음 필드를 가진다.

- `id`
- `email`
- `nickname`
- `password`
- `introduce`
- `commonDate`

하지만 `MemberDocumentDto`는 현재 `email`만 가진다.

rules에는 Entity와 DTO를 1:1로 매칭하고, 일부 필드만 조회하는 경우 조회하지 않은 DTO 필드는 null로 둘 수 있다고 되어 있다.

따라서 `MemberDocumentDto`도 `MemberDocument`와 동일한 필드 구조를 가지는 쪽이 컨벤션에 더 맞다.

### 정리 방향

- `MemberDocumentDto`에 `id`, `email`, `nickname`, `password`, `introduce`, `commonDate` 필드를 추가한다.
- `MemberDocumentDto.from(MemberDocument)`에서 builder로 전체 필드를 매핑한다.
- 로그인 흐름에서는 필요한 `email`만 사용한다.

## 불일치 3. Response 정적 팩토리

Response 객체는 불변과 Builder는 적용되어 있지만, Service와 Handler에서 생성자를 직접 호출하고 있다.

예시:

```java
new CreateMemberResponse(uuid, Integer.parseInt(verifyCode))
```

```java
new VerifyMemberResponse("access", "refresh")
```

```java
new LoginResponse("access-token", "refresh-token")
```

rules에는 DTO, Request, Response의 정적 팩토리 메소드는 Builder 패턴을 이용해 작성한다고 되어 있다.

### 정리 방향

- `CreateMemberResponse.of(uuid, verifyCode)`를 추가한다.
- `VerifyMemberResponse.of(accessToken, refreshToken)`를 추가한다.
- `LoginResponse.of(accessToken, refreshToken)`를 추가한다.
- Service와 Handler, Security Handler에서는 생성자 대신 정적 팩토리를 사용한다.

## 불일치 4. 빈 Request/Response 객체

`FindMemberRequest`, `FindMemberResponse`는 현재 빈 일반 class다.

기존 Kotlin 코드도 빈 클래스였지만, Java 컨벤션 기준으로는 Request/Response 객체를 불변으로 작성하는 규칙과 다소 애매하게 맞물린다.

### 정리 방향

- 당장 필드가 없다면 `final` class로 변경한다.
- 추후 필드가 생기면 `@Getter`, `@Builder`, `private final` 필드 구조로 확장한다.

## 불일치 5. 테스트 규칙

현재 테스트는 context load 중심이다.

rules에는 다음 규칙이 있다.

```text
모든 함수 단위로 단위 테스트 코드 작성
모든 API 단위로 통합 테스트 코드 작성
```

따라서 현재 상태는 “컴파일과 컨텍스트 검증은 됨”이지, 테스트 규칙을 모두 만족한 상태는 아니다.

### 정리 방향

- Handler 함수 단위 테스트를 작성한다.
- Service 흐름 단위 테스트를 작성한다.
- 회원가입, 회원 인증, 로그인 API 통합 테스트를 작성한다.
- Redis, PostgreSQL, Elasticsearch 의존성이 있으므로 테스트 전략을 별도 계획으로 나눌 수 있다.

---

# Builder 활용 부족

> builder를 활용하지 않는것도 전반적으로 아쉬움. 아마 kotlin이 생성자를 쓰니까 그거 그대로 java도 생성자 쓰는걸로 옮긴거 같은데

## 답변

지적한 내용이 맞다.

이번 Java 이관은 Kotlin `data class(...)`와 생성자 호출을 Java 생성자로 거의 1:1 대응시킨 형태가 많다.

그래서 컴파일은 되지만, rules의 핵심인 “DTO, Request, Response 정적 팩토리 + Builder 기반 생성” 스타일과는 결이 덜 맞는다.

## 아쉬운 지점

다음과 같은 직접 생성자 호출이 남아 있다.

- `new CreateMemberResponse(...)`
- `new VerifyMemberResponse(...)`
- `new LoginResponse(...)`
- `new CommonResponse<>(...)`
- `new TokenDetails(...)`
- `new CommonValidateResult(...)`
- `new MemberEntityDto(...)` 사용 가능성
- `new CommonDateDocument(...)` 사용 가능성

## 컨벤션 방향

외부 호출부에서는 생성자를 직접 사용하지 않고 의미 있는 정적 팩토리를 사용하는 쪽이 좋다.

예시:

```java
CreateMemberResponse.of(uuid, verifyCode)
VerifyMemberResponse.of(accessToken, refreshToken)
LoginResponse.of(accessToken, refreshToken)
CommonResponse.ok(data)
CommonResponse.created(data)
CommonResponse.error(status, message)
TokenDetails.from(member)
```

객체 내부에서는 builder를 이용해 생성한다.

```java
public static CreateMemberResponse of(
        String uuid,
        int verifyCode
) {
    return CreateMemberResponse.builder()
            .uuid(uuid)
            .verifyCode(verifyCode)
            .build();
}
```

## CommonResponse 개선 방향

`CommonResponse`는 현재 호출부마다 다음처럼 응답 코드, 이름, 메시지를 직접 조합한다.

```java
new CommonResponse<>(code, name, message, data)
```

이 방식은 호출부가 지저분하고 응답 정책이 여러 곳에 흩어진다.

다음처럼 공통 정적 팩토리를 제공하는 방향이 좋다.

```java
CommonResponse.ok(data)
CommonResponse.created(data)
CommonResponse.error(status, message)
```

이렇게 하면 Service, Handler, Security Handler 코드가 더 간결해지고 응답 정책도 한 곳에 모인다.

## 판단

현재 코드는 “Kotlin을 Java 문법으로 옮긴 상태”에 가깝다.

다음 정리 단계에서는 “Java 프로젝트 컨벤션에 맞게 리모델링”하는 작업이 필요하다.

---

# 코드 컨벤션 수정 계획

> 이제 코드 컨벤션 수정계획 이야기해봐

## 답변

현재 상태는 “컴파일되는 Java 이관본”이고, 다음 단계는 이를 “프로젝트 rules에 맞는 Java 코드”로 다듬는 작업이다.

한 번에 모두 수정하기보다 컴파일 안정성을 유지하면서 단계적으로 정리하는 방향이 좋다.

## 1단계. 요청 바인딩 정리

가장 먼저 Controller 요청 바인딩을 확정한다.

`verify`는 JSON body 요청이 자연스럽기 때문에 `@RequestBody`를 적용한다.

```java
public CommonResponse<VerifyMemberResponse> verify(
        @Valid @RequestBody VerifyMemberRequest payload
)
```

`sign-up`은 `CreateMemberRequest`에 `MultipartFile profile`이 있어 선택이 필요하다.

- 회원가입에서 프로필 파일을 같이 받을 경우 `@ModelAttribute`
- JSON 회원가입으로 갈 경우 `profile`을 제거하거나 프로필 업로드 API를 분리하고 `@RequestBody`

현재 Kotlin 원본도 이 지점이 애매했으므로, 보수적으로는 다음 방향이 자연스럽다.

- `sign-up`: `@ModelAttribute`
- `verify`: `@RequestBody`

## 2단계. 데이터 객체 생성 방식을 Builder + 정적 팩토리로 정리

컨벤션 수정의 중심은 Response, DTO, Request의 생성 방식을 정리하는 것이다.

우선 Response부터 정리한다.

```java
CreateMemberResponse.of(uuid, verifyCode)
VerifyMemberResponse.of(accessToken, refreshToken)
LoginResponse.of(accessToken, refreshToken)
```

객체 내부에서는 builder를 사용한다.

```java
public static VerifyMemberResponse of(
        String accessToken,
        String refreshToken
) {
    return VerifyMemberResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
}
```

그 다음 DTO도 같은 방향으로 정리한다.

- `MemberEntityDto.from(entity)`
- `MemberEntityDto.of(...)`
- `MemberDocumentDto.from(document)`
- `TokenDetails.from(member)`

외부 호출부에서는 생성자를 직접 호출하지 않도록 줄인다.

## 3단계. CommonResponse 응답 정책 정리

현재는 여러 위치에서 `new CommonResponse<>(...)`를 직접 사용한다.

이 방식은 동작은 하지만 응답 정책이 여러 곳에 흩어진다.

다음과 같은 정적 팩토리로 응답 정책을 모으는 것이 좋다.

```java
CommonResponse.ok(data)
CommonResponse.created(data)
CommonResponse.error(status, message)
```

그러면 Service 호출부는 다음처럼 간결해진다.

```java
return CommonResponse.ok(response);
```

상태 코드, 코드명, 기본 메시지 정책도 `CommonResponse` 한 곳에서 관리할 수 있다.

## 4단계. DTO 1:1 매칭과 빈 객체 정리

`MemberDocumentDto`는 `MemberDocument`와 1:1로 매칭되도록 필드를 확장한다.

- `id`
- `email`
- `nickname`
- `password`
- `introduce`
- `commonDate`

`FindMemberRequest`, `FindMemberResponse`는 당장 필드가 없더라도 `final`로 변경한다.

추후 필드가 생기면 builder 기반 불변 객체로 확장한다.

## 추천 작업 순서

1. `@RequestBody`, `@ModelAttribute` 정리
2. Response 정적 팩토리 추가 후 호출부 변경
3. `CommonResponse` 정적 팩토리 추가 후 호출부 변경
4. DTO 정적 팩토리와 Builder 사용 정리
5. `MemberDocumentDto` 1:1 매칭
6. 빈 Request/Response `final` 처리
7. `compileJava`, `compileTestJava`, `test`
8. 작업 내용을 `task3.md`에 기록

## 테스트 보강 분리

테스트 보강은 별도 작업으로 분리하는 것이 좋다.

현재 컨벤션 정리는 구조 개선 작업이고, 테스트 규칙까지 한 번에 모두 만족시키려면 범위가 크게 늘어난다.

우선 이번 컨벤션 정리는 컴파일과 기존 context test 유지까지를 완료 기준으로 잡는다.

함수 단위 테스트와 API 통합 테스트는 다음 계획으로 분리한다.

---

# 수정 우선순위

1. Controller 요청 바인딩 정리
   - `verify`: `@RequestBody`
   - `sign-up`: `@ModelAttribute` 또는 API 설계 재결정

2. `MemberDocumentDto` 1:1 필드 매칭
   - `MemberDocument`와 동일한 필드 구조로 확장
   - `from()` 전체 필드 매핑

3. Response 정적 팩토리 추가
   - 생성자 직접 호출 제거
   - `of()` 또는 `from()` 정적 팩토리 + builder 사용

4. CommonResponse 정적 팩토리 추가
   - `ok(data)`, `created(data)`, `error(status, message)` 같은 응답 정책 메소드 추가
   - 호출부의 `new CommonResponse<>(...)` 직접 호출 제거

5. 전반적인 생성자 직접 호출 정리
   - DTO, Request, Response는 외부에서 생성자를 직접 호출하지 않도록 정리
   - 정적 팩토리 내부에서 builder 사용

6. 빈 Request/Response 객체 정리
   - `FindMemberRequest`, `FindMemberResponse`를 `final` class로 변경

7. 테스트 보강
   - 함수 단위 테스트
   - API 단위 통합 테스트
