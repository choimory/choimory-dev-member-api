# 목차

- [개요](#개요)
- [작업 요약](#작업-요약)
  - [분석](#분석)
  - [구현](#구현)
  - [패키지 정리](#패키지-정리)
  - [테스트](#테스트)
  - [알아둘 점](#알아둘-점)

---

# 개요

- 프론트(local, dev, prod)에서 member-api를 호출할 수 있도록 CORS를 설정했다.
- 분석과 계획은 plan1.md, plan2.md, 구현 내용은 task1.md, task2.md 참고.

---

# 작업 요약

## 분석

- 허용 Origin: local `http://localhost:3000`, dev `http://localhost:3000`과 `https://dev.choimory.dev`, prod `https://choimory.dev`
- 허용 메서드 GET, POST, PUT, PATCH, DELETE, OPTIONS / 허용 헤더 `Authorization`, `Content-Type` / 노출 헤더 `Authorization` / `maxAge` 3600초 / `allowCredentials` 미사용
- Security가 `CorsFilter`를 인증 필터보다 앞에 두도록 `SecurityConfig`에서 CORS를 켜야 preflight가 401로 막히지 않는다.

## 구현

- `CorsProperties`: `cors.allowed-origins`를 바인딩하는 레코드. `@NotEmpty`로 설정 누락 시 기동 실패
- `CorsConfig`: `corsConfigurationSource` 빈을 `/**`에 등록하고 메서드, 헤더, `maxAge` 등은 상수로 둠
- `SecurityConfig`: TODO를 지우고 `.cors(Customizer.withDefaults())` 추가, `CorsProperties` 등록
- `application-{local,dev,prod}.yml`: 환경별 `cors.allowed-origins` 추가

## 패키지 정리

- `JwtProperties`와 `CorsProperties`를 `config.properties` 패키지로 옮겨 설정 바인딩 객체의 위치를 통일했다.
- `CorsPropertiesTest`도 같은 패키지로 옮기고 관련 import를 수정했다.

## 테스트

- `CorsTest` 5개: 허용 Origin preflight 200과 헤더, 비허용 Origin·메서드·헤더 preflight 403, 401 응답의 CORS 헤더, 비허용 Origin 실제 요청 403
- `CorsPropertiesTest` 4개: 환경별 yml의 Origin 목록, 스킴과 끝 슬래시 형식
- 전체 테스트 통과 (`./gradlew test`)

## 알아둘 점

- `ChoimoryDevMemberApiApplicationTests`에 `@ActiveProfiles("local")`을 추가했다.
- 프로필 없이 실행하면 CORS Origin이 없어 앱이 기동되지 않는다. 배포 시 프로필을 지정해야 한다.
- README에 CORS 설명, `config/properties` 패키지, 테스트 목록을 반영했다.

---
