# 목차

- [개요](#개요)
- [구현 진행해줘 테스트도](#구현-진행해줘-테스트도)
  - [구현 내용](#구현-내용)
    - [CorsProperties](#corsproperties)
    - [CorsConfig](#corsconfig)
    - [SecurityConfig](#securityconfig)
    - [환경별 yml](#환경별-yml)
  - [테스트](#테스트)
    - [CorsTest](#corstest)
    - [CorsPropertiesTest](#corspropertiestest)
  - [테스트 결과](#테스트-결과)
  - [알아둘 점](#알아둘-점)

---

# 개요

- plan1.md의 분석과 계획에 따라 CORS 설정을 구현한다.
- 허용 Origin은 환경별 프로퍼티로 분리하고, 메서드와 헤더 등은 코드의 상수로 둔다.
- 자세한 분석은 plan1.md 참고.

---

# 구현 진행해줘 테스트도

> 구현 진행해줘 테스트도

## 답변

계획대로 CORS 설정을 구현하고 테스트를 추가했다. 전체 테스트가 통과했다.

## 구현 내용

### CorsProperties

- 파일: `member-api/src/main/java/dev/choimory/member/api/config/CorsProperties.java`
- `@ConfigurationProperties(prefix = "cors")` 레코드이며 `allowedOrigins`(`List<String>`)만 받는다.
- `@Validated`와 `@NotEmpty`를 걸어, Origin 설정이 없으면 앱이 기동되지 않는다.

### CorsConfig

- 파일: `member-api/src/main/java/dev/choimory/member/api/config/CorsConfig.java`
- `corsConfigurationSource` 빈을 `/**`에 등록한다.
- 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
- 허용 헤더: `Authorization`, `Content-Type`
- 노출 헤더: `Authorization`
- `allowCredentials`: false
- `maxAge`: 3600초
- 허용 Origin만 `CorsProperties`에서 받고, 나머지는 환경과 관계없이 같아 상수로 둔다.

### SecurityConfig

- 파일: `member-api/src/main/java/dev/choimory/member/api/config/SecurityConfig.java`
- "TODO 클라이언트가 정해지면 CORS를 설정한다." 주석을 지우고 `.cors(Customizer.withDefaults())`를 추가했다. 빈 이름이 `corsConfigurationSource`라서 `CorsConfig`의 설정이 자동으로 연결된다.
- `@EnableConfigurationProperties`에 `CorsProperties`를 추가했다.
- Security가 `CorsFilter`를 인증 필터보다 앞에 두므로 preflight(OPTIONS)가 401로 막히지 않는다.

### 환경별 yml

- `application-local.yml`: `http://localhost:3000`
- `application-dev.yml`: `http://localhost:3000`, `https://dev.choimory.dev`
- `application-prod.yml`: `https://choimory.dev`

```yaml
# CORS 허용 Origin. 프론트 주소(스킴, 포트 포함)
cors:
  allowed-origins:
    - http://localhost:3000
```

## 테스트

### CorsTest

- 파일: `member-api/src/test/java/dev/choimory/member/api/security/CorsTest.java`
- `SecurityConfigTest`와 같은 `@SpringBootTest` + MockMvc + local 프로파일(Testcontainers) 구성이다.
- 5개 테스트
  - 허용 Origin의 preflight는 200이고 허용 메서드, 헤더, `maxAge`가 응답에 담기며 `Allow-Credentials` 헤더는 없다.
  - 허용하지 않은 Origin의 preflight는 403이고 `Allow-Origin` 헤더가 없다.
  - 허용하지 않은 메서드(TRACE)와 헤더(X-Custom)를 요청하는 preflight는 403이다.
  - 인증 실패(401) 응답에도 `Allow-Origin`과 `Expose-Headers`가 붙는다.
  - 허용하지 않은 Origin의 실제 요청은 403이다.

### CorsPropertiesTest

- 파일: `member-api/src/test/java/dev/choimory/member/api/config/CorsPropertiesTest.java`
- 컨텍스트를 띄우지 않고 환경별 yml을 읽어 `CorsProperties`로 바인딩한다.
- 4개 테스트
  - local, dev, prod 각각의 허용 Origin이 계획과 같다. (파라미터 테스트 3개)
  - 모든 허용 Origin이 스킴을 포함하고 끝에 슬래시가 없다.

## 테스트 결과

- 전체 테스트가 통과했다. (`./gradlew test`, BUILD SUCCESSFUL)
- 새로 추가한 테스트는 `CorsTest` 5개, `CorsPropertiesTest` 4개로 모두 통과했다.

## 알아둘 점

- 기존 `ChoimoryDevMemberApiApplicationTests`에 `@ActiveProfiles("local")`을 추가했다. 프로파일 없이 실행하면 CORS Origin이 없어 컨텍스트 로딩이 실패하기 때문이며, 다른 통합 테스트와 같은 방식이다.
- 프로파일을 지정하지 않고 앱을 실행하면 이제 기동에 실패한다. 의도한 동작이며, 배포 시에는 `prod` 등의 프로파일을 지정해야 한다.
- README 반영과 done 문서 작성, calendar의 DONE 변경은 작업 마무리 단계에서 진행한다.

---
