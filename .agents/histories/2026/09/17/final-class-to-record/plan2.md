# 목차

- [개요](#개요)
- [우아한형제들 기술블로그 참고](#우아한형제들-기술블로그-참고)
- [Redis record 직렬화 대응 방향](#redis-record-직렬화-대응-방향)
- [우아한형제들 방식 적용 방안](#우아한형제들-방식-적용-방안)
- [기존 Redis 데이터 호환성](#기존-redis-데이터-호환성)
- [일반 JSON 직렬화와 역직렬화](#일반-json-직렬화와-역직렬화)
- [구현 전 계획](#구현-전-계획)

---

# 개요

- `final class` 데이터 객체들을 record로 전환하면서 Redis/Jackson 역직렬화 문제가 발생할 수 있는지 추가로 검토했다.
- 특히 `MemberEntityDto`는 `RedisTemplate<String, Object>`로 Redis에 저장하고 다시 조회하는 객체라 record 전환 후 런타임 직렬화 검증이 필요하다.
- `compileJava`로는 발견되지 않는 런타임 문제일 수 있으므로, 작업 마무리 전에 Redis record 직렬화 대응을 먼저 검토하기로 했다.

---

# 우아한형제들 기술블로그 참고

> https://techblog.woowahan.com/22767/ 이글 한번 봐봐

## 답변

우아한형제들 기술블로그의 “Spring Cache(@Cacheable) + Spring Data Redis 사용 시 record 직렬화 오류 원인과 해결” 글을 확인했다.

## 핵심 내용

해당 글의 핵심은 `record`와 `GenericJackson2JsonRedisSerializer` 조합에서 Redis 역직렬화 문제가 발생할 수 있다는 점이다.

문제가 되는 대표 조건은 다음과 같다.

- Spring Cache `@Cacheable` 사용
- Spring Data Redis 사용
- `GenericJackson2JsonRedisSerializer` 사용
- ObjectMapper에 `DefaultTyping.NON_FINAL` 설정
- 캐시 대상 객체가 Java record

Java record는 자동으로 final 클래스가 된다. Jackson의 `DefaultTyping.NON_FINAL` 설정은 final이 아닌 클래스에만 타입 정보인 `@class`를 붙이므로, record에는 타입 정보가 생략될 수 있다.

그 결과 Redis에 저장된 JSON에 타입 정보가 없고, 캐시 조회 시 `Object` 타입에서 실제 record 타입을 복원하지 못해 역직렬화 오류가 발생할 수 있다.

이 문제는 컴파일 타임에는 드러나지 않고, 첫 요청은 정상 동작한 뒤 두 번째 캐시 조회 시점에 런타임 오류로 드러날 수 있다.

## 이 프로젝트와의 관련성

현재 이 프로젝트에서는 `@Cacheable` 계열 사용은 발견되지 않았다.

하지만 다음 구조가 존재한다.

- `RedisTemplate<String, Object>` 사용
- `GenericJackson2JsonRedisSerializer` 사용
- `MemberEntityDto`를 Redis에 저장
- Redis에서 값을 `Object`로 조회 후 `instanceof MemberEntityDto`로 확인

따라서 글의 상황과 완전히 같지는 않지만, `record + GenericJackson2JsonRedisSerializer + Object 역직렬화`라는 핵심 위험 지점은 겹친다.

현재 `RedisConfig`는 `new GenericJackson2JsonRedisSerializer()` 기본 생성자를 사용하고 있어 커스텀 `ObjectMapper`의 `DefaultTyping.NON_FINAL` 문제가 그대로 재현될지는 확인이 필요하다.

그래도 `MemberEntityDto`를 record로 전환한 상태에서는 Redis 직렬화/역직렬화 검증 없이 마무리하는 것은 안전하지 않다.

---

# Redis record 직렬화 대응 방향

> 앞으로 작업 시작하면 오류날수 있어서 jackson의 final class 역직렬화 문제를 미리 해결해놓고 가는게 좋을거 같은데

## 답변

record 전환 작업을 마무리하기 전에 Jackson final class 역직렬화 문제를 미리 해결하거나 최소한 재현 여부를 검증하는 방향이 좋다.

현재 상태에서는 `compileJava`가 성공하더라도 Redis 런타임 역직렬화에서 오류가 발생할 수 있다.

특히 `MemberEntityDto`는 인증 대기 회원 정보로 Redis에 저장되고 다시 조회되는 객체이므로 우선 검증 대상이다.

## 진행 계획

1. 현재 `RedisConfig`를 다시 확인한다.
   - 현재는 `new GenericJackson2JsonRedisSerializer()` 기본 생성자를 사용한다.
   - RedisTemplate value serializer가 어떤 타입 정보를 저장하는지 확인한다.

2. `MemberEntityDto` record 직렬화 테스트를 추가한다.
   - `GenericJackson2JsonRedisSerializer`로 `MemberEntityDto`를 serialize 한다.
   - 다시 deserialize 했을 때 `MemberEntityDto`로 복원되는지 확인한다.
   - 실패하면 record 역직렬화 문제가 실제로 재현된 것이다.

3. 실패하거나 위험이 확인되면 Redis용 ObjectMapper 설정을 추가한다.
   - record에도 타입 정보가 붙도록 TypeResolver를 커스터마이징한다.
   - 또는 현재 RedisTemplate 구조에서 record가 안정적으로 복원되도록 serializer 설정을 변경한다.

4. 기존 Redis 데이터 호환성을 고려한다.
   - 직렬화 포맷이 바뀌면 기존 Redis 데이터와 충돌할 수 있다.
   - 현재 인증 대기 데이터는 TTL 3분이라 영향은 비교적 작다.
   - 그래도 배포 시점에는 기존 key 만료를 기다리거나 key prefix/version 변경을 검토할 수 있다.

5. 검증한다.
   - `./gradlew compileJava`
   - Redis serializer 단위 테스트
   - 필요 시 `./gradlew compileTestJava`
   - 필요 시 `./gradlew test`

## 선택지

### 선택지 1. Redis record 직렬화 대응 구현

record 전환 방향을 유지하고 Redis serializer 설정을 record 친화적으로 보강한다.

장점은 record 전환 원칙을 유지할 수 있고, 앞으로 `@Cacheable`이나 Redis 저장 대상에 record가 추가되어도 같은 문제를 예방할 수 있다는 점이다.

단점은 Redis 직렬화 설정 코드가 늘어나고, 기존 Redis 데이터와의 호환성을 확인해야 한다는 점이다.

### 선택지 2. Redis 저장 객체만 class 유지

`MemberEntityDto`처럼 Redis에 `Object`로 저장/조회되는 객체만 record 전환에서 제외하고 기존 class 구조를 유지한다.

장점은 Redis 직렬화 설정 변경 없이 보수적으로 갈 수 있다는 점이다.

단점은 record 전환 기준이 일관되지 않고, 앞으로 다른 record 객체가 Redis나 Cache에 들어갈 때 같은 문제가 다시 발생할 수 있다는 점이다.

## 추천 방향

추천 방향은 선택지 1이다.

이번 작업에서 record 전환을 전체 데이터 객체 기준으로 진행하고 있으므로, Redis 직렬화 설정도 record를 안정적으로 처리할 수 있게 미리 보강하는 편이 좋다.

다만 구현 전에 먼저 현재 serializer 기본 동작을 테스트로 확인하고, 실제로 문제가 재현되는지 확인한 뒤 설정 변경 범위를 확정하는 것이 안전하다.

---

# 우아한형제들 방식 적용 방안

> 내가 준 링크에선 어떻게 해결했대?

## 답변

해당 글에서는 record에도 Jackson 타입 정보가 붙도록 Redis용 `ObjectMapper`의 타입 판별 로직을 커스터마이징해서 해결했다.

문제 원인은 `GenericJackson2JsonRedisSerializer`가 Redis에서 값을 `Object`로 꺼낼 때 JSON 내부의 타입 정보인 `@class`를 보고 실제 타입을 복원해야 하는데, record는 final 클래스라 `DefaultTyping.NON_FINAL` 조건에서 제외된다는 점이다.

해결 방식은 `DefaultTypeResolverBuilder`를 상속해 `useForType()`을 오버라이드하는 것이다.

핵심 로직은 다음과 같다.

```java
if (t.getRawClass().isRecord()) {
    return true;
}
return super.useForType(t);
```

즉, 기존 `NON_FINAL` 정책은 유지하되 record인 경우만 예외적으로 타입 정보를 붙이게 만든다.

그 다음 이 커스텀 TypeResolver를 Redis용 `ObjectMapper`에 적용한다.

```java
RecordSupportingTypeResolver typeResolver =
        new RecordSupportingTypeResolver(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());

StdTypeResolverBuilder initializedResolver =
        typeResolver.init(JsonTypeInfo.Id.CLASS, null);

initializedResolver = initializedResolver.inclusion(JsonTypeInfo.As.PROPERTY);

mapper.setDefaultTyping(initializedResolver);
```

이렇게 하면 Redis에 저장되는 record JSON에도 `@class` 같은 타입 정보가 포함되고, `GenericJackson2JsonRedisSerializer`가 캐시 조회 시 record 타입을 정상 복원할 수 있다.

`DefaultTyping.EVERYTHING`도 해결책으로 언급되지만 추천하지 않는다. 모든 타입에 타입 정보가 붙어 JSON이 커지고 성능상 이점이 없으며 deprecated 방향이기 때문이다.

`record` 대신 class를 사용하는 방법도 현실적인 우회책이지만, 최종 해결책은 record만 예외 처리하는 TypeResolver 방식이다.

---

> 그 방식을 우리도 적용한다면 어떻게 ?

## 답변

우리 프로젝트에 해당 방식을 적용한다면 `RedisConfig`에서 `GenericJackson2JsonRedisSerializer`를 생성할 때 record를 지원하는 Redis 전용 `ObjectMapper`를 주입하는 형태가 된다.

현재 구조는 다음과 같다.

```java
template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
```

이를 다음 방향으로 변경한다.

```java
template.setValueSerializer(
        new GenericJackson2JsonRedisSerializer(redisObjectMapper())
);
```

그리고 `redisObjectMapper()` 안에서 record에도 타입 정보가 붙도록 `DefaultTypeResolverBuilder`를 커스터마이징한다.

예상 구조는 다음과 같다.

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        return template;
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        PolymorphicTypeValidator validator = objectMapper.getPolymorphicTypeValidator();

        StdTypeResolverBuilder typeResolver = new RecordSupportingTypeResolver(
                ObjectMapper.DefaultTyping.NON_FINAL,
                validator
        ).init(JsonTypeInfo.Id.CLASS, null);

        typeResolver = typeResolver.inclusion(JsonTypeInfo.As.PROPERTY);

        objectMapper.setDefaultTyping(typeResolver);
        return objectMapper;
    }

    private static final class RecordSupportingTypeResolver extends ObjectMapper.DefaultTypeResolverBuilder {

        private RecordSupportingTypeResolver(
                ObjectMapper.DefaultTyping defaultTyping,
                PolymorphicTypeValidator validator
        ) {
            super(defaultTyping, validator);
        }

        @Override
        public boolean useForType(JavaType type) {
            if (type.getRawClass().isRecord()) {
                return true;
            }
            return super.useForType(type);
        }
    }
}
```

이렇게 하면 `MemberEntityDto` 같은 record도 Redis에 저장될 때 타입 정보가 붙고, Redis에서 `Object`로 꺼냈을 때 `MemberEntityDto`로 복원될 가능성이 높다.

기존 코드의 다음 흐름도 유지할 수 있다.

```java
Object member = redisTemplate.opsForValue().get(uuid + ":" + verifyCode);

if (member instanceof MemberEntityDto memberEntityDto && email.equals(memberEntityDto.email())) {
    return memberEntityDto;
}
```

주의할 점은 다음과 같다.

- 기존 Redis 데이터와 호환성이 깨질 수 있다.
- 이 설정은 `RedisTemplate<String, Object>` 전체 value serializer에 적용된다.
- Spring Data Redis 버전에 따라 `GenericJackson2JsonRedisSerializer()` 기본 생성자의 내부 default typing 동작이 다를 수 있다.

추천 적용 순서는 다음과 같다.

1. `MemberEntityDto` Redis serializer round-trip 테스트를 작성한다.
2. 현재 기본 serializer로 실패/성공 여부를 확인한다.
3. `RedisConfig`에 record-supporting ObjectMapper를 적용한다.
4. 같은 테스트가 통과하는지 확인한다.
5. `compileJava`, `compileTestJava`, 필요 시 `test`를 실행한다.

---

# 기존 Redis 데이터 호환성

> 지금 말은 기존에 이미 redis에 올라간 애들이랑 호환성이 깨진다는거야?

## 답변

정확히는 기존에 Redis에 이미 올라간 데이터와 호환성이 깨질 수 있다는 뜻이다.

이유는 Redis에 저장되는 JSON 형태가 바뀔 수 있기 때문이다.

기존 serializer가 타입 정보 없이 다음처럼 저장했다고 가정할 수 있다.

```json
{
  "id": "uuid",
  "email": "a@test.com",
  "nickname": "nick",
  "password": "encoded",
  "introduce": "hi"
}
```

record 대응 설정을 넣으면 타입 정보가 포함되어 다음처럼 저장될 수 있다.

```json
{
  "@class": "dev.choimory.member.api.member.v1.command.domain.dto.MemberEntityDto",
  "id": "uuid",
  "email": "a@test.com",
  "nickname": "nick",
  "password": "encoded",
  "introduce": "hi"
}
```

또는 설정에 따라 wrapper array 형태가 될 수도 있다.

새 serializer가 타입 정보를 기대하는데 Redis에 이미 저장된 옛 데이터에는 `@class`가 없으면 역직렬화 실패가 발생할 수 있다. 반대로 옛 serializer가 새 포맷을 읽지 못하는 경우도 생길 수 있다.

다만 이 프로젝트에서는 영향이 작아 보인다.

현재 Redis에 저장하는 주요 대상은 회원가입 인증 대기 정보이고 TTL이 3분이다.

```java
memberCommandHandler.setWaitVerifyMember(member, verifyCode, 3, TimeUnit.MINUTES);
```

따라서 배포 시점에 이미 Redis에 있던 인증 대기 데이터는 최대 3분 정도만 문제 가능성이 있다. 만료되면 사라진다.

최악의 경우 그 사이에 인증하려는 사용자가 인증 실패를 겪을 수 있다.

대응 선택지는 다음과 같다.

- 배포 전후 3분 정도 기존 인증 대기 key 만료를 고려한다.
- Redis key prefix/version을 바꿔 새 포맷과 옛 포맷이 섞이지 않게 한다.
- 기존 값 읽기 실패 시 null 처리 후 재가입/재요청을 유도한다.

현재 로직은 Redis에서 값을 못 읽거나 타입이 맞지 않으면 인증 실패로 이어질 가능성이 크다. 따라서 엄밀히는 기존 Redis 데이터와 직렬화 포맷 호환성 이슈가 맞다.

---

> 그럼 지금 개발 시작 전 단계니까 문제없지?

## 답변

현재 개발 시작 전 단계라면 사실상 문제없다고 볼 수 있다.

이 프로젝트의 Redis 저장 대상은 현재 회원가입 인증 대기 데이터이고 TTL도 3분이다. 운영 중 장기 보존 데이터처럼 마이그레이션을 고민할 성격은 아니다.

아직 실제 운영 데이터가 없거나 작업 시작 전 단계라면 Redis 직렬화 포맷을 지금 바꿔두는 것이 오히려 좋다.

정리하면 다음과 같다.

- 기존 Redis 데이터가 없으면 문제 없다.
- 기존 데이터가 있어도 TTL 3분짜리 인증 대기 데이터라 영향이 작다.
- 지금 바꿔두면 앞으로 record 저장 시 런타임 오류를 예방할 수 있다.
- key versioning이나 호환 레이어까지는 과한 대응일 가능성이 크다.

따라서 이 시점에서는 `RedisConfig`에 record 지원 ObjectMapper를 미리 넣고, serializer round-trip 테스트까지 해두는 방향이 좋다.

---

# 일반 JSON 직렬화와 역직렬화

> redis말고 json 직렬화/역직렬화는 문제 없어? 객체에 @jsonproperty 붙여놔서 괜찮나?

## 답변

Redis 말고 일반 JSON 요청/응답 직렬화와 역직렬화는 대체로 문제 없을 가능성이 높다.

이유는 현재 프로젝트가 `jackson-databind:2.17.0`을 사용하고 있고, Jackson은 이미 Java record를 기본 지원하기 때문이다.

일반적인 Controller 요청/응답처럼 대상 타입이 명확한 상황에서는 record component 이름을 보고 canonical constructor를 호출할 수 있다.

예시는 다음과 같다.

```java
public record LoginRequest(
        @JsonProperty("email") String email,
        @JsonProperty("password") String password
) {
}
```

Jackson은 `LoginRequest.class`라는 목표 타입을 알고 있으므로 JSON의 `email`, `password`를 record canonical constructor에 매핑할 수 있다.

`@JsonProperty`를 record component에 붙여둔 것도 이름 매핑을 명확하게 해주므로 도움이 된다.

다만 주의할 점은 다음과 같다.

1. 일반 JSON은 괜찮지만, `Object`로 역직렬화하는 경우가 위험하다.
   - Redis 문제가 여기에 해당한다.
   - 대상 타입이 `Object.class`면 Jackson이 무슨 타입으로 만들어야 하는지 JSON 안의 타입 정보를 봐야 한다.

2. `MultipartFile`이 있는 `CreateMemberRequest`는 별도 확인이 필요하다.
   - JSON body로 `MultipartFile`을 받는 것은 일반적인 구조가 아니다.
   - `multipart/form-data`라면 `@RequestBody`보다 `@ModelAttribute` 또는 `@RequestPart`가 맞을 수 있다.
   - 이 문제는 record 자체보다는 기존 API 바인딩 설계 문제에 가깝다.

3. Validation annotation 위치는 record component에 붙이면 된다.
   - `@NotBlank String email` 형태는 Spring Validation에서 보통 정상 인식된다.

4. `@JsonCreator`는 없어도 된다.
   - record는 canonical constructor가 있고 Jackson이 record metadata를 인식한다.
   - component에 `@JsonProperty`가 있으면 보통 충분하다.
   - 필요하다면 compact constructor나 canonical constructor에 `@JsonCreator`를 명시할 수 있지만, 현재 단계에서는 과해 보인다.

정리하면 다음과 같다.

- 일반 API Request/Response JSON은 대체로 괜찮다.
- `@JsonProperty`를 붙여둔 것은 좋은 선택이다.
- 위험한 것은 `Object` 타입 기반 역직렬화, 즉 Redis/Cache/다형성 구조다.
- `CreateMemberRequest.profile`은 record 문제가 아니라 multipart 바인딩 관점에서 따로 봐야 한다.

---

> jackson databind 버전이 record 지원하는버전이면 @jsonproperty 이건 왜쓰는거야? 안써도 되지 않나?

## 답변

Jackson databind가 record를 지원하는 버전이면 대부분의 경우 `@JsonProperty`는 없어도 된다.

Jackson 2.12 이상에서는 record를 기본 지원한다.

예를 들어 다음 record는 별도 annotation 없이도 일반적인 JSON 직렬화와 역직렬화가 가능하다.

```java
public record LoginRequest(
        String email,
        String password
) {
}
```

다음 JSON은 record component 이름인 `email`, `password`에 매핑된다.

```json
{
  "email": "a@test.com",
  "password": "1234"
}
```

`@JsonProperty`를 사용하는 대표 이유는 다음과 같다.

1. JSON 필드명과 Java component명이 다를 때 사용한다.

```java
public record LoginRequest(
        @JsonProperty("user_email") String email
) {
}
```

2. 이름 매핑을 명시해서 리팩토링이나 설정 변화에 덜 흔들리게 하려는 경우 사용한다.
3. 기존 class에서 생성자 파라미터에 `@JsonProperty`를 붙이던 스타일을 record로 옮겨온 경우 사용한다.
4. `ObjectMapper` naming strategy가 들어갔을 때 특정 JSON 필드명을 고정하고 싶은 경우 사용한다.
5. 코드에서 외부 JSON 계약을 명시적으로 보여주고 싶은 경우 사용한다.

하지만 현재 코드처럼 JSON 필드명과 record component명이 완전히 같다면 기능적으로는 없어도 된다.

---

> 아 objectmapper로 직접 직렬화/역직렬화 할때는 필요한건가?

## 답변

`ObjectMapper`로 직접 직렬화/역직렬화할 때도 대상 타입이 명확하면 `@JsonProperty`는 보통 필요 없다.

예를 들어 다음 record가 있다고 하면,

```java
public record LoginRequest(
        String email,
        String password
) {
}
```

다음처럼 직접 역직렬화할 수 있다.

```java
ObjectMapper objectMapper = new ObjectMapper();

LoginRequest request = objectMapper.readValue(
        """
        {
          "email": "a@test.com",
          "password": "1234"
        }
        """,
        LoginRequest.class
);
```

Jackson은 `LoginRequest.class`라는 대상 타입을 알고 있으므로 record component 이름을 기준으로 매핑한다.

직렬화도 마찬가지다.

```java
String json = objectMapper.writeValueAsString(
        new LoginRequest("a@test.com", "1234")
);
```

결과는 보통 다음처럼 나온다.

```json
{"email":"a@test.com","password":"1234"}
```

`@JsonProperty`가 필요한 경우는 직접 `ObjectMapper`를 사용하는지 여부가 아니라 다음과 같은 경우다.

- JSON 이름과 Java 이름이 다를 때
- naming strategy와 무관하게 특정 JSON 이름을 고정하고 싶을 때
- `@JsonProperty(access = ...)`, `required`, `defaultValue` 같은 속성을 쓰고 싶을 때
- `@JsonAlias` 같은 명시 매핑과 함께 계약을 분명히 하고 싶을 때
- record가 아니라 일반 class 생성자 파라미터 매핑에서 파라미터 이름 정보를 안정적으로 보장하고 싶을 때

현재 record들처럼 component 이름과 JSON 필드명이 같고, `ObjectMapper.readValue(json, LoginRequest.class)`처럼 대상 타입을 넘기는 구조라면 없어도 된다.

Redis 문제가 생기는 이유는 `@JsonProperty` 유무 때문이 아니라 `Object.class`처럼 대상 타입이 불명확한 상태에서 역직렬화하기 때문이다.

---

> 그럼 제거해도 될거같은데?

## 답변

현재 record component의 `@JsonProperty`는 제거해도 괜찮아 보인다.

이유는 다음과 같다.

- JSON 필드명과 record component명이 동일하다.
- 현재 Jackson 버전은 record를 지원한다.
- 대상 타입이 명확한 일반 API 요청/응답에서는 component 이름 기반 매핑이 가능하다.
- 제거하면 record 코드가 훨씬 간결해진다.

예를 들어 다음 형태에서,

```java
public record LoginRequest(
        @JsonProperty("email") String email,
        @JsonProperty("password") String password
) {
}
```

다음 형태로 줄일 수 있다.

```java
public record LoginRequest(
        String email,
        String password
) {
}
```

다만 Redis/Jackson 타입 정보 문제와 `@JsonProperty` 제거는 별개다.

Redis 문제는 `Object` 역직렬화 시 타입 정보를 못 찾는 문제이므로, component annotation을 제거해도 해결되거나 악화되는 종류의 문제가 아니다.

진행한다면 범위는 다음과 같다.

- record component의 `@JsonProperty(...)` 제거
- 더 이상 사용하지 않는 `com.fasterxml.jackson.annotation.JsonProperty` import 제거
- `./gradlew compileJava`로 확인

---

# 구현 전 계획

> 이제 plan2 내용들 구현 시작할건데 전에 계획부터 얘기해봐

## 답변

`plan2` 구현은 Redis/Jackson record 역직렬화 안전성 확보와 불필요한 `@JsonProperty` 제거를 목표로 한다.

## 진행 순서

1. 현재 상태를 재확인한다.
   - `RedisConfig`의 serializer 설정을 확인한다.
   - record 객체들에 붙은 `@JsonProperty` import와 annotation을 확인한다.
   - `MemberEntityDto`가 Redis 저장/조회에 쓰이는 흐름을 다시 확인한다.

2. `@JsonProperty`를 제거한다.
   - record component 이름과 JSON 필드명이 같은 객체들에서 `@JsonProperty(...)`를 제거한다.
   - 더 이상 사용하지 않는 `JsonProperty` import를 제거한다.
   - 대상은 현재 record로 전환한 DTO, Request, Response, 값 객체들이다.
   - `@NotBlank` 같은 validation annotation은 유지한다.

3. Redis serializer round-trip 테스트를 추가한다.
   - `GenericJackson2JsonRedisSerializer` 기본 설정으로 `MemberEntityDto`를 serialize/deserialize 해보는 테스트를 작성한다.
   - 목적은 현재 기본 serializer가 record를 `Object`로 복원할 수 있는지 확인하는 것이다.
   - 이 테스트는 설정 변경 전 실패할 가능성이 있다.
   - 실패를 먼저 확인하면 Redis serializer 설정이 필요한 이유가 명확해진다.

4. `RedisConfig`에 record 지원 ObjectMapper를 적용한다.
   - `RedisTemplate<String, Object>`의 value serializer를 변경한다.
   - 기존 방식은 다음과 같다.

```java
new GenericJackson2JsonRedisSerializer()
```

   - 변경 방향은 다음과 같다.

```java
new GenericJackson2JsonRedisSerializer(redisObjectMapper())
```

   - `redisObjectMapper()`에서 record도 타입 정보를 갖도록 TypeResolver를 커스터마이징한다.
   - 우아한형제들 기술블로그 방식처럼 `DefaultTypeResolverBuilder`를 상속한 내부 클래스를 추가한다.
   - `useForType(JavaType type)`에서 `type.getRawClass().isRecord()`면 true를 반환한다.
   - 기존 `NON_FINAL` 정책은 유지한다.

5. 테스트를 보강한다.
   - 동일한 `MemberEntityDto` round-trip 테스트가 변경 후 성공하는지 확인한다.
   - 가능하면 저장 JSON에 타입 정보가 포함되는지도 확인한다.
   - Redis 서버를 띄우지 않는 serializer 단위 테스트로 먼저 처리한다.
   - 실제 RedisTemplate 통합 테스트는 필요하면 다음 단계로 분리한다.

6. 검증한다.
   - `./gradlew compileJava`
   - `./gradlew compileTestJava`
   - 가능하면 serializer 테스트만 먼저 실행한다.
   - 전체 `test`는 시간이 걸리거나 Testcontainers 영향을 받을 수 있으니 실행 전에 다시 확인한다.

7. 문서 기록은 구현 후 사용자 요청에 따라 진행한다.
   - 구현 후 바로 문서를 쓰지 않고 먼저 결과를 설명한다.
   - 사용자가 요청하면 `task2.md`에 작업 내용을 기록한다.
   - 마무리 단계에서 `done1.md`, `calendar.md`의 `DONE` 변경, `README.md` 반영 여부를 처리한다.

## 구현 범위

구현 대상 파일은 다음 정도로 예상한다.

- `RedisConfig.java`
- record DTO, Request, Response 파일들의 `@JsonProperty` 제거
- Redis serializer 테스트 파일 1개 추가

## 판단점

핵심 판단점은 기본 serializer에서 실패하는 것을 먼저 테스트로 확인할지, 아니면 위험을 전제로 바로 record 지원 ObjectMapper를 넣고 성공 테스트만 작성할지다.

추천은 기본 serializer 실패 여부를 먼저 테스트로 확인하는 것이다.

이렇게 하면 나중에 Redis record 지원 설정이 왜 들어갔는지 근거가 남는다.
