# 목차

- [개요](#개요)
- [분석 ㄱㄱ](#분석-ㄱㄱ)
  - [현재 상태](#현재-상태)
  - [구현 계획](#구현-계획)

---

# 개요

- CORS 작업을 진행한다.
- 분석과 계획은 대화를 통해 정리하며, 요청 시 이 문서에 추가한다.
- 허용할 도메인
  - 로컬: http://localhost:3000
  - 개발: http://localhost:3000, https://dev.choimory.dev
  - 운영: https://choimory.dev
- 허용 Origin: 환경별 프론트 주소(local, dev, prod)
- 허용 헤더: Authorization, Content-Type
- 허용 메서드: 필요한 것(GET, POST, PUT, PATCH, DELETE, OPTIONS)
- 자격증명: 쿠키가 member-api로 가지 않으니 allowCredentials는 필요 없어요.
- Spring Security 필터 체인: Security를 쓰고 있어서 SecurityConfig에서 CORS를 같이 켜야 해요. MVC 설정만으로는 preflight(OPTIONS)가 401로 막힐 수 있어요.
- 노출 헤더(exposedHeaders): 토큰은 body로 내려주지만 Authorization을 exposedHeaders로 함께 처리
- maxAge: 3600초
- 환경별 Origin 목록은 application-{profile}.yml 프로퍼티로 분리하여 주입

---

# 분석 ㄱㄱ

> 분석 ㄱㄱ

## 답변

코드를 읽고 분석한 결과, 현재 상태와 구현 계획은 아래와 같다.

## 현재 상태

- `SecurityConfig.java`에 "TODO 클라이언트가 정해지면 CORS를 설정한다."가 남아 있고 CORS 설정은 없다.
- `/v1/member/join`, `/v1/login`, `/v1/logout`, `/v1/member/reissue`(POST)만 `permitAll`이고, 나머지는 인증이 필요하다.
- 필터 체인은 세션 없는 stateless 구조이고 CSRF는 꺼져 있다. 필터 순서는 `JwtAuthenticationFilter`, `LoginFilter`, `LogoutFilter`다.
- `.cors()`를 켜지 않으면 브라우저의 preflight(OPTIONS)가 `permitAll` 대상이 아니라서 401이 된다. `.cors()`를 켜면 Security가 `CorsFilter`를 인증 필터보다 앞에 두므로 해결된다.
- 설정 프로퍼티는 `JwtProperties` 레코드에 `@ConfigurationProperties`와 `@Validated`를 쓰고, `SecurityConfig`의 `@EnableConfigurationProperties`로 등록하는 패턴이다.
- 프로파일은 `local`, `dev`, `prod` 세 개이고, 지정하지 않으면 `prod`가 된다. 환경별 설정은 `application-{profile}.yml`에 있다.
- 토큰은 `Authorization: Bearer` 헤더로 받는다. 로그인과 재발급 응답의 토큰은 body에 담겨 있다.

## 구현 계획

### CorsProperties 레코드 추가

- `@ConfigurationProperties(prefix = "cors")`로 `allowedOrigins`(`List<String>`)만 받는다.
- `@NotEmpty`를 걸어 설정 누락 시 기동이 실패하게 한다.
- 메서드, 헤더, `maxAge`는 환경마다 달라지지 않으므로 코드의 상수로 둔다.

### CorsConfig 클래스 추가

- `config` 패키지에 두고, `corsConfigurationSource` 빈을 `/**`에 등록한다.
- 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
- 허용 헤더: `Authorization`, `Content-Type`
- 노출 헤더: `Authorization`
- `allowCredentials`: false
- `maxAge`: 3600초
- `SecurityConfig`가 이미 큰 편이라 별도 클래스로 분리한다.

### SecurityConfig 수정

- TODO 주석을 지우고 `.cors(Customizer.withDefaults())`를 추가한다. 빈 이름이 `corsConfigurationSource`라서 자동으로 연결된다.
- `CorsProperties`를 `@EnableConfigurationProperties`에 등록한다.

### yml 수정

- `application-local.yml`: `http://localhost:3000`
- `application-dev.yml`: `http://localhost:3000`, `https://dev.choimory.dev`
- `application-prod.yml`: `https://choimory.dev`

### 테스트 추가

- `CorsTest`: `SecurityConfigTest`와 같은 `@SpringBootTest` + MockMvc + local 프로파일
  - 허용 Origin의 preflight는 200이고 허용 메서드와 헤더, `maxAge`가 응답에 담긴다.
  - 허용하지 않은 Origin의 preflight는 403이다.
  - 인증이 필요한 주소의 401 응답에도 `Access-Control-Allow-Origin`과 `Access-Control-Expose-Headers`가 붙는다. 브라우저가 401 본문을 읽으려면 필요하다.
  - `allowCredentials`를 켜지 않으므로 `Access-Control-Allow-Credentials` 헤더는 없다.
  - dev와 prod 설정은 `CorsProperties` 바인딩 테스트로 확인한다. 해당 yml을 읽어 Origin 목록이 계획과 같은지 본다.
- 테스트는 Docker가 필요한 Testcontainers 기반이다.

### 마무리

- README의 config 목록과 `SecurityConfig` 설명, 프로퍼티 설명에 CORS를 반영한다.
- calendar를 DONE으로 바꾸고 done 문서를 작성하는 일은 마무리 단계에서 진행한다.

---
