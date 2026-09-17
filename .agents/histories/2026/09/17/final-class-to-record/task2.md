# 목차

- [개요](#개요)
- [plan2 구현 시작](#plan2-구현-시작)
- [구현 내용](#구현-내용)
- [검증 결과](#검증-결과)

---

# 개요

- `plan2.md`에 정리한 Redis/Jackson record 역직렬화 대응과 `@JsonProperty` 제거 작업을 구현했다.
- record component 이름과 JSON 필드명이 동일한 객체들에서 불필요한 `@JsonProperty`를 제거했다.
- Redis에 record를 `Object`로 저장하고 조회할 때 타입 정보를 잃지 않도록 Redis 전용 ObjectMapper 설정을 추가했다.
- Redis serializer 단위 테스트를 추가해 `MemberEntityDto` record가 타입 정보와 함께 직렬화되고 다시 같은 record 타입으로 복원되는지 확인했다.

---

# plan2 구현 시작

> 구현시작

## 답변

`plan2` 구현을 시작했다.

진행 전 계획은 다음과 같았다.

1. 관련 파일을 다시 확인한다.
   - `RedisConfig`
   - record DTO, Request, Response
   - `MemberEntityDto` Redis 저장/조회 흐름
2. record component의 불필요한 `@JsonProperty`를 제거한다.
3. Redis record 역직렬화 대응을 `RedisConfig`에 추가한다.
4. Redis serializer 단위 테스트를 추가한다.
5. 컴파일과 테스트를 실행해 검증한다.

---

# 구현 내용

## JsonProperty 제거

record component 이름과 JSON 필드명이 동일한 객체들에서 `@JsonProperty(...)`를 제거했다.

대상은 다음과 같다.

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

제거 이유는 다음과 같다.

- Jackson 2.17.0은 Java record를 기본 지원한다.
- 대상 타입이 명확한 일반 JSON 직렬화/역직렬화에서는 record component 이름으로 매핑할 수 있다.
- 현재 JSON 필드명과 record component명이 동일하다.
- Redis record 역직렬화 문제는 `@JsonProperty` 유무가 아니라 `Object` 기반 역직렬화의 타입 정보 문제다.

`CreateMemberRequest`의 `@NotBlank` validation annotation은 유지했다.

## RedisConfig 변경

`RedisTemplate<String, Object>`의 value serializer가 record 타입 정보를 포함하는 Redis 전용 ObjectMapper를 사용하도록 변경했다.

기존 방식은 다음과 같았다.

```java
template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
```

변경 후에는 `GenericJackson2JsonRedisSerializer` Bean을 별도로 만들고, RedisTemplate에 주입한다.

```java
template.setValueSerializer(redisValueSerializer);
```

추가한 설정은 다음과 같다.

- `redisValueSerializer()` Bean 추가
- `redisObjectMapper()` 추가
- `RecordSupportingTypeResolver` 추가

`RecordSupportingTypeResolver`는 Jackson의 `ObjectMapper.DefaultTypeResolverBuilder`를 상속한다.

기존 `DefaultTyping.NON_FINAL` 정책은 유지하되, record 타입은 final 클래스이더라도 타입 정보를 포함하도록 `useForType()`을 오버라이드했다.

핵심 로직은 다음과 같다.

```java
@Override
public boolean useForType(JavaType type) {
    if (type.getRawClass().isRecord()) {
        return true;
    }
    return super.useForType(type);
}
```

이 설정으로 `MemberEntityDto` 같은 record가 Redis JSON으로 저장될 때 `@class` 타입 정보가 포함된다.

## Redis serializer 테스트 추가

`RedisConfigTest`를 추가했다.

테스트 내용은 다음과 같다.

1. `RedisConfig().redisValueSerializer()`로 Redis JSON Value Serializer를 생성한다.
2. `MemberEntityDto` record를 builder로 생성한다.
3. serializer로 byte array로 직렬화한다.
4. 직렬화된 JSON에 `@class` 타입 정보와 `MemberEntityDto` 클래스명이 포함되는지 확인한다.
5. 다시 역직렬화한다.
6. 결과가 `MemberEntityDto` 타입이고 원본 record와 같은 값인지 확인한다.

---

# 검증 결과

## 정적 확인

`git diff --check`를 실행해 공백 오류가 없음을 확인했다.

`rg`로 `JsonProperty`, `@JsonCreator`, `@JsonProperty` 잔여 사용이 없는지 확인했다.

## 컴파일 검증

다음 명령을 실행했다.

```shell
./gradlew compileJava compileTestJava
```

결과는 성공이다.

```text
BUILD SUCCESSFUL
```

## Redis serializer 단위 테스트

다음 명령을 실행했다.

```shell
./gradlew test --tests dev.choimory.member.api.config.RedisConfigTest
```

결과는 성공이다.

```text
BUILD SUCCESSFUL
```

## 미실행 항목

전체 테스트는 아직 실행하지 않았다.

```shell
./gradlew test
```
