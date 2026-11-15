# 목차

- [개요](#개요)
- [작업 마무리](#작업-마무리)
- [작업 요약](#작업-요약)
- [검증 결과](#검증-결과)
- [참고 사항](#참고-사항)

---

# 개요

- `member-api`에 도메인별 API 기본 구조(Controller, Service, Handler, Repository)를 클래스와 필드까지 작성했다.
- 공통 응답 객체를 `member-domain`에, 공통 예외와 예외 핸들러를 `member-api`에 추가했다.
- README.md에 API 구조와 공통 응답, 예외를 반영했다.

---

# 작업 마무리

> 작업 마무리하자

## 답변

작업 마무리를 진행했다. `done1.md`를 작성하고 `.agents/histories/calendar.md`의 작업 상태를 `DONE`으로 변경했으며, `README.md`에 변경사항을 반영했다.

---

# 작업 요약

## API 기본 구조 (plan1, task1)

- 도메인 4개(member, authority, agreement, menu)에 `dev.choimory.member.api.{domain}.v1.{command|query}.{controller|service|handler|repository}` 구조로 클래스 28개(Controller 8, Service 8, Handler 8, Repository 4)를 만들었다.
- 이름은 `{Domain}{Command|Query}{Layer}`이고 Repository는 `{Domain}Repository`다. command와 query의 Handler 이름 충돌(Spring 빈 이름)을 피하기 위해 Command, Query를 붙였다.
- 클래스 간 의존은 `@RequiredArgsConstructor`와 `private final` 필드로 연결했고, query Handler는 Repository가 없어 필드가 없다.
- query repository 패키지는 `.gitkeep`만 두었다. (ES 조회로 구현 예정)
- Controller에 `@RequestMapping("/v1/{domain}")`을 적용했다.
- 메소드는 만들지 않았다.

## 공통 응답과 예외 (plan2, task2)

- member-domain `common/command/response`: `CommonResponse<T>`, `CommonValidateResult` (record + `@Builder(toBuilder = true)`)
- member-api `common/exception`: `CommonException`, `CommonExceptionHandler` (예전 코드 8754490d 기반)
- 공통 요청, 공통 페이징 요청, 페이지 응답은 제외했다.
- 핸들러 수정: `RuntimeException` 핸들러 중복을 제거하고, 500 응답에서 예외 메시지를 노출하지 않도록 했다.

---

# 검증 결과

- `spotlessApply`와 `:member-api:compileJava`로 API 기본 구조 클래스가 컴파일되는 것을 확인했다. (메소드가 없어 테스트는 작성하지 않았다)
- 공통 응답과 예외는 단위 테스트 14개(`CommonResponseTest` 3, `CommonValidateResultTest` 2, `CommonExceptionTest` 2, `CommonExceptionHandlerTest` 7)를 작성했고, `DtoRulesTest`와 함께 실행해 통과했다.
- 전체 테스트(`./gradlew clean build`)는 실행하지 않았다.

---

# 참고 사항

- `AuthenticationException` 핸들러는 `data`에 예외 메시지를 넣는 방식을 그대로 두었다. 수정 여부는 추후 결정한다.
- 시큐리티 필터에서 던지는 인증 예외는 `@RestControllerAdvice`에 잡히지 않는 경우가 많아, 로그인과 시큐리티 작업 시 별도 처리가 필요할 수 있다.
- authority에서 메뉴 정보가 필요하면 Service에서 menu Handler를 호출한다. (Handler는 자기 도메인 Repository만 호출한다)
- 작업 내용이 바뀐 부분은 task2.md의 "예외 핸들러 수정" 섹션에 있다. 같은 문서 구현 섹션의 핸들러 표는 수정 전 상태다.

---
