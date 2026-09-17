# 목차

- [개요](#개요)
- [작업 마무리](#작업-마무리)
- [작업 요약](#작업-요약)
- [검증 결과](#검증-결과)
- [참고 사항](#참고-사항)

---

# 개요

- final class 기반 데이터 객체를 Java record로 전환하는 작업을 마무리했다.
- DTO, Request, Response, 값 객체는 `record + @Builder(toBuilder = true)` 기준으로 정리했다.
- Redis에 record를 `Object`로 저장하고 조회할 때 발생할 수 있는 Jackson 타입 정보 문제를 Redis 전용 ObjectMapper 설정으로 대응했다.
- README.md에 변경된 코드 규칙과 Redis record 직렬화 주의사항을 반영했다.

---

# 작업 마무리

> 작업 마무리하자

## 답변

작업 마무리를 진행했다.

마무리 작업으로 `done1.md`를 작성하고, `.agents/histories/calendar.md`의 작업 상태를 `DONE`으로 변경했으며, `README.md`에 이번 작업으로 바뀐 기술 규칙을 반영했다.

---

# 작업 요약

## record 전환

다음 데이터 객체들을 Java record로 전환했다.

- `CommonDateDocument`
- `CommonValidateResult`
- `CommonResponse<T>`
- `LoginRequest`
- `LoginResponse`
- `TokenDetails`
- `MemberEntityDto`
- `MemberDocumentDto`
- `CreateMemberRequest`
- `VerifyMemberRequest`
- `CreateMemberResponse`
- `VerifyMemberResponse`
- `FindMemberRequest`
- `FindMemberResponse`

전환 기준은 다음과 같다.

- `record + @Builder(toBuilder = true)`를 사용한다.
- 정적 팩토리 메소드 내부 생성은 builder를 사용한다.
- record 접근자는 `email()`, `verifyCode()`처럼 component 이름 기반으로 호출한다.
- Entity, Elasticsearch root Document, Exception, Util 클래스는 record 전환에서 제외한다.

## JsonProperty 제거

record component 이름과 JSON 필드명이 동일한 객체들에서 `@JsonProperty`를 제거했다.

Jackson 2.17.0은 Java record를 지원하므로, 대상 타입이 명확한 일반 JSON 직렬화/역직렬화에서는 component 이름 기반 매핑을 사용할 수 있다.

## Redis record 직렬화 대응

`RedisConfig`에 Redis 전용 `GenericJackson2JsonRedisSerializer` Bean을 추가했다.

Redis 전용 ObjectMapper는 기존 `DefaultTyping.NON_FINAL` 정책을 유지하되, record 타입에도 타입 정보가 포함되도록 `RecordSupportingTypeResolver`를 사용한다.

이 설정으로 `MemberEntityDto` 같은 record가 Redis JSON에 저장될 때 `@class` 타입 정보가 포함되고, `RedisTemplate<String, Object>`로 다시 조회할 때 실제 record 타입으로 복원할 수 있다.

## 테스트 추가

`RedisConfigTest`를 추가했다.

테스트는 `MemberEntityDto` record를 Redis serializer로 직렬화한 뒤 다음을 확인한다.

- JSON에 `@class` 타입 정보가 포함되는지
- `MemberEntityDto` 클래스명이 포함되는지
- 역직렬화 결과가 `MemberEntityDto` 타입인지
- 역직렬화 결과가 원본 record와 같은 값인지

## README 반영

README.md에 다음 내용을 반영했다.

- DTO, Request, Response는 Java record와 `@Builder(toBuilder = true)`를 사용한다.
- 데이터 객체의 정적 팩토리 메소드 내부 생성은 Builder 패턴을 사용한다.
- RedisTemplate은 record 타입 정보를 Redis JSON에 포함하는 Redis 전용 ObjectMapper를 사용한다.
- record component 이름과 JSON 필드명이 동일한 경우 별도 `@JsonProperty`를 작성하지 않는다.
- Redis serializer 단위 테스트 명령을 검증 목록에 추가했다.

---

# 검증 결과

## 성공한 검증

다음 명령을 실행했고 성공했다.

```shell
./gradlew compileJava
```

```shell
./gradlew compileJava compileTestJava
```

```shell
./gradlew test --tests dev.choimory.member.api.config.RedisConfigTest
```

또한 `git diff --check`로 공백 오류가 없음을 확인했다.

## 미실행 검증

전체 테스트는 실행하지 않았다.

```shell
./gradlew test
```

---

# 참고 사항

- `MemberQueryHandler`에서 `MemberDocument`를 record로 착각하고 `password()`를 호출해 컴파일 오류가 발생했으나, `MemberDocument`는 record 전환 제외 대상이므로 `getPassword()`로 수정했다.
- 기존 Redis 데이터와 새 serializer 포맷의 호환성은 깨질 수 있지만, 현재 Redis 저장 대상은 회원가입 인증 대기 데이터이고 TTL이 3분이라 개발 초기 단계에서는 영향이 작다고 판단했다.
- `CreateMemberRequest.profile`은 record 전환 문제와 별개로 multipart 바인딩 관점에서 추후 확인이 필요하다.
