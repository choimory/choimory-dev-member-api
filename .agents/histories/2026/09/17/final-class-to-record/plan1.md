# 목차

- [개요](#개요)
- [분석시작](#분석시작)
- [현재 final class 목록](#현재-final-class-목록)
- [record 전환 후보 분류](#record-전환-후보-분류)
- [record 전환 시 확인할 문제](#record-전환-시-확인할-문제)
- [작업 계획](#작업-계획)
- [검증 계획](#검증-계획)
- [record Builder 사용 방향](#record-builder-사용-방향)
- [record toBuilder 사용 방향](#record-tobuilder-사용-방향)

---

# 개요

- final class들을 record로 변경할거임
- 데이터 객체를 record로 사용할때 문제 생기는 점들이 뭐뭐 있지? 이를테면 @cacheable 사용할때 같은 문제들도 확인해놓기

---

# 분석시작

> 분석시작

## 답변

final class를 record로 변경하기 위한 사전 분석을 시작했다.

README.md와 백엔드 규칙을 확인한 결과, 이 프로젝트는 Java 21 기반 Spring Boot 회원 API이며 DTO, 요청 객체, 응답 객체를 불변 객체로 작성하는 규칙을 가진다.

현재 코드 규칙 예시는 Lombok `@Getter`, `@Builder(toBuilder = true)`, `final class` 기반이다. record로 전환하면 불변성 자체는 더 단순해지지만, 기존 getter 호출 방식과 Builder 사용 방식이 바뀌므로 호출부 수정이 함께 필요하다.

---

# 현재 final class 목록

현재 `src/main/java` 기준 `final class`는 다음 15개다.

- `LoginRequest`
- `LoginResponse`
- `TimeUnitUtil`
- `TokenDetails`
- `CommonValidateResult`
- `CommonResponse<T>`
- `CommonDateDocument`
- `FindMemberRequest`
- `MemberDocumentDto`
- `FindMemberResponse`
- `CreateMemberRequest`
- `MemberEntityDto`
- `VerifyMemberRequest`
- `CreateMemberResponse`
- `VerifyMemberResponse`

현재 코드에서 `@Cacheable`, `@CachePut`, `@CacheEvict`, `@CacheConfig` 사용은 발견되지 않았다.

---

# record 전환 후보 분류

## 우선 전환 후보

DTO, Request, Response처럼 데이터 전달 목적이 강하고 상태 변경이 없는 객체는 record 전환 후보로 볼 수 있다.

- `LoginRequest`
- `LoginResponse`
- `TokenDetails`
- `CommonValidateResult`
- `CommonResponse<T>`
- `CommonDateDocument`
- `MemberDocumentDto`
- `CreateMemberRequest`
- `MemberEntityDto`
- `VerifyMemberRequest`
- `CreateMemberResponse`
- `VerifyMemberResponse`

## 보류 후보

다음 객체는 현재 비어 있는 미구현 객체라 record로 바꿀 수는 있지만, API 설계가 아직 확정되지 않은 상태로 보인다.

- `FindMemberRequest`
- `FindMemberResponse`

## 제외 후보

다음 객체는 record 전환 대상이 아니다.

- `TimeUnitUtil`
  - 상태를 가지는 데이터 객체가 아니라 private 생성자를 가진 유틸리티 클래스다.

현재 검색된 `final class` 목록에는 JPA Entity, Elasticsearch 루트 Document, Spring Bean은 포함되지 않았다. 이런 객체들은 프록시, 리플렉션, 기본 생성자, 프레임워크 매핑 제약이 있으므로 record 전환 대상에서 제외하는 것이 안전하다.

---

# record 전환 시 확인할 문제

## 접근자 변경

record는 Lombok getter를 만들지 않고 컴포넌트명과 같은 접근자를 제공한다.

- 기존: `request.getEmail()`
- 변경: `request.email()`

따라서 record로 전환하면 서비스, 핸들러, 필터, 정적 팩토리 메소드 내부 호출부를 함께 수정해야 한다.

현재 확인된 주요 호출부는 다음과 같다.

- `LoginProcessFilter`
- `MemberCommandService`
- `MemberCommandHandler`
- `MemberQueryHandler`
- `TokenDetails`
- `MemberEntityDto`
- `MemberDocumentDto`
- `CommonExceptionHandler`

## Builder 제거 또는 대체

record는 기본적으로 Lombok `@Builder(toBuilder = true)`와 같은 사용 흐름을 제공하지 않는다.

현재 `.builder()` 사용은 다음 객체에서 발견된다.

- `LoginResponse`
- `CommonValidateResult`
- `TokenDetails`
- `MemberEntityDto`
- `MemberDocumentDto`
- `CreateMemberResponse`
- `VerifyMemberResponse`

전환 방식은 둘 중 하나로 정해야 한다.

- record의 canonical constructor를 직접 호출한다.
- Lombok `@Builder`를 record에 유지한다.

프로젝트 규칙에는 DTO, Request, Response의 정적 팩토리 메소드를 Builder 패턴으로 작성한다고 되어 있으므로, record 전환과 함께 이 규칙을 변경할지 여부도 결정이 필요하다.

## Jackson 직렬화와 역직렬화

record는 Jackson 2.12 이상에서 기본 지원된다. 현재 `jackson-databind:2.17.0`을 사용하므로 일반적인 JSON 직렬화/역직렬화는 가능하다.

다만 현재 코드에는 `@JsonCreator`, `@JsonProperty`가 생성자에 명시되어 있다. record 전환 시에는 record component에 annotation을 둘지, Jackson 기본 record 지원에 맡길지 정해야 한다.

요청 객체와 Redis 저장 객체는 역직렬화 검증이 필요하다.

- `LoginRequest`
- `CreateMemberRequest`
- `VerifyMemberRequest`
- `MemberEntityDto`

## Redis 직렬화

현재 Redis는 `GenericJackson2JsonRedisSerializer`를 사용하며, 인증 대기 회원 정보로 `MemberEntityDto`를 저장하고 다시 꺼낸다.

record 전환 시 확인해야 할 점은 다음과 같다.

- Redis에 저장된 JSON을 record로 다시 읽을 수 있는지
- `Object member` 조회 후 `member instanceof MemberEntityDto memberEntityDto` 패턴이 그대로 동작하는지
- 기존에 저장된 final class 기반 JSON과 record 기반 JSON의 호환성이 필요한지

현재 운영 중 기존 Redis 데이터와 호환해야 한다면 이 부분이 가장 먼저 검증되어야 한다.

## Spring Cache 관련

현재 코드에서는 `@Cacheable` 계열 사용이 발견되지 않았다.

일반적으로 record는 값 기반 `equals/hashCode`를 자동 제공하므로 캐시 key 객체로는 오히려 유리한 편이다. 다만 다음 경우는 주의가 필요하다.

- SpEL에서 getter 이름을 직접 참조하는 경우
- 캐시 key 생성이 JavaBean getter 관례에 의존하는 경우
- 직렬화 캐시에 Java class metadata가 들어가는 경우
- record 전환 전후로 `serialVersionUID` 또는 클래스 구조 호환성이 필요한 경우

현재 프로젝트에서는 Spring Cache보다 Redis JSON 직렬화와 getter 호출부 변경이 더 직접적인 리스크다.

## Bean Validation

record 전환 시 `@NotBlank` 같은 validation annotation은 필드가 아니라 record component에 붙이는 방식으로 변경해야 한다.

대상은 현재 기준 다음과 같다.

- `CreateMemberRequest.email`
- `CreateMemberRequest.password`
- `CreateMemberRequest.nickname`

## MultipartFile 포함 요청 객체

`CreateMemberRequest`는 `MultipartFile profile`을 가진다.

record로 전환하는 것은 가능하지만, 실제 요청이 JSON인지 multipart/form-data인지에 따라 바인딩 방식 확인이 필요하다. 현재 컨트롤러 바인딩 방식과 함께 검증해야 한다.

---

# 작업 계획

1. record 전환 대상과 제외 대상을 확정한다.
2. 호출부 영향이 작은 객체부터 전환한다.
   - `CommonValidateResult`
   - `LoginResponse`
   - `CreateMemberResponse`
   - `VerifyMemberResponse`
   - `TokenDetails`
3. Redis 직렬화 영향이 있는 객체는 별도 단계로 전환한다.
   - `MemberEntityDto`
4. 요청 바인딩 영향이 있는 객체는 별도 단계로 전환한다.
   - `LoginRequest`
   - `CreateMemberRequest`
   - `VerifyMemberRequest`
5. 조회 DTO와 공통 응답 객체를 전환한다.
   - `MemberDocumentDto`
   - `CommonDateDocument`
   - `CommonResponse<T>`
6. 비어 있는 query request/response는 유지할지 record로 바꿀지 별도 결정한다.
   - `FindMemberRequest`
   - `FindMemberResponse`
7. `TimeUnitUtil`은 record 전환 대상에서 제외한다.

---

# 검증 계획

구현 단계에서 최소한 다음 검증이 필요하다.

1. `./gradlew compileJava`
2. `./gradlew compileTestJava`
3. `./gradlew test`

추가로 Redis 직렬화 관련 검증이 필요하다.

- `MemberEntityDto` 저장 후 조회 시 record 타입으로 복원되는지 확인
- 기존 JSON payload와 record 역직렬화 호환성 확인

이번 분석 단계에서는 테스트 실행은 하지 않았다.

---

# record Builder 사용 방향

> record에 builder를 넣을수 있는지, 넣는게 좋은지 말해봐

## 답변

record에도 Lombok `@Builder`를 사용할 수 있다.

```java
@Builder
public record CreateMemberResponse(
        String uuid,
        int verifyCode
) {
}
```

이렇게 작성하면 기존 class 기반 객체처럼 builder를 통해 생성할 수 있다.

```java
CreateMemberResponse response = CreateMemberResponse.builder()
        .uuid(uuid)
        .verifyCode(verifyCode)
        .build();
```

초기 판단으로는 필드가 적은 record에는 builder 없이 canonical constructor나 정적 팩토리를 사용하는 방식도 가능하다고 봤다.

하지만 프로젝트 백엔드 규칙에는 DTO, Request, Response의 정적 팩토리 메소드를 Builder 패턴으로 작성한다는 규칙이 있다. 또한 일부 필드만 조회하는 경우 DTO의 조회하지 않은 필드는 null로 둘 수 있다는 규칙도 있다.

따라서 이 프로젝트에서는 record로 전환하더라도 builder를 유지하는 것이 규칙과 더 잘 맞는다.

---

> 내가 생각하는 builder의 장점은 원하는 필드만 초기화하여 생성할 수 있다는건데 record 생성자는 그게 돼 ?

## 답변

record의 canonical constructor만으로는 원하는 필드만 초기화할 수 없다.

record의 canonical constructor는 모든 record component 값을 반드시 받아야 한다.

```java
public record MemberEntityDto(
        String id,
        String email,
        String nickname,
        String password,
        String introduce
) {
}
```

위 record는 항상 전체 값을 넣어 생성해야 한다.

```java
new MemberEntityDto(id, email, nickname, password, introduce);
```

일부 필드만 넣으려면 나머지 값에 명시적으로 `null`을 넣어야 한다.

```java
new MemberEntityDto(id, email, null, null, null);
```

builder를 사용하면 원하는 필드만 지정할 수 있다.

```java
MemberEntityDto dto = MemberEntityDto.builder()
        .id(id)
        .email(email)
        .build();
```

이 프로젝트는 일부 필드 조회 시 조회하지 않은 DTO 필드를 null로 둘 수 있다는 규칙이 있으므로, DTO에는 builder가 특히 의미 있다.

---

> record도 builder쓰자. 필드가 2,3개밖에 안된다고 해도 향후엔 늘어날수도 있기 때문에

## 답변

record 전환 시에도 builder를 사용한다.

필드가 현재 2~3개뿐인 객체도 향후 필드가 늘어날 수 있으므로, 일관성과 확장 가능성을 위해 `record + @Builder` 형태로 통일한다.

기준은 다음과 같다.

- DTO, Request, Response, 기타 데이터 객체는 `record + @Builder`로 전환한다.
- 기존 정적 팩토리 메소드는 유지한다.
- 정적 팩토리 내부에서도 직접 생성자 호출 대신 builder를 사용한다.
- record component에 validation, Jackson annotation을 배치한다.
- 기존 getter 호출부는 record 접근자로 변경한다.
  - `getEmail()` → `email()`
  - `getVerifyCode()` → `verifyCode()`
- `TimeUnitUtil`, Entity, Spring Bean, Exception은 record 전환 대상에서 제외한다.

예시는 다음과 같다.

```java
@Builder(toBuilder = true)
public record VerifyMemberResponse(
        String accessToken,
        String refreshToken
) {
    public static VerifyMemberResponse of(
            String accessToken,
            String refreshToken
    ) {
        return VerifyMemberResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
```

이 방향이면 기존 백엔드 규칙의 Builder 패턴 사용도 유지할 수 있다.

---

# record toBuilder 사용 방향

> tobuilder의 사용여부는 쓰는게 좋을까 안쓰는게 좋을까

## 답변

이 프로젝트에서는 `toBuilder = true`를 기본으로 유지한다.

DTO, Request, Response는 지금 필드가 적어도 나중에 늘어날 수 있고, 특히 DTO는 일부 필드만 조회한 객체나 기존 객체에서 몇 필드만 바꾼 파생 객체가 생길 수 있다.

record는 불변 객체이므로 값을 바꾸려면 새 객체를 만들어야 한다. 이때 `toBuilder()`는 기존 값을 보존한 채 필요한 값만 바꿔 새 객체를 만드는 용도에 맞다.

```java
MemberEntityDto changed = member.toBuilder()
        .nickname("newNickname")
        .build();
```

다만 `toBuilder()`를 비즈니스 상태 변경 용도로 남용하면 DTO를 여러 곳에서 조금씩 바꾸는 흐름이 생길 수 있다. 이 프로젝트 규칙상 Entity 수정은 Entity 내부 함수로 처리하고, DTO는 데이터 전달과 조합 용도로만 사용해야 한다.

적용 기준은 다음과 같다.

- DTO는 `@Builder(toBuilder = true)`를 사용한다.
- Response도 `@Builder(toBuilder = true)`를 사용한다.
- Request도 전체 일관성을 위해 `@Builder(toBuilder = true)`를 사용한다.
- `CommonResponse<T>`도 `@Builder(toBuilder = true)`를 사용한다.
- 단, Entity 수정이나 비즈니스 상태 변경을 DTO `toBuilder()`로 처리하지 않는다.

최종 방향은 `record + @Builder(toBuilder = true)`로 통일한다.
