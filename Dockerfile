# member-api 빌드 Stage
# Gradle은 이미지에 설치하지 않고 프로젝트의 wrapper(gradlew) 버전을 그대로 쓴다.
FROM eclipse-temurin:21.0.13_9-jdk-noble AS builder

WORKDIR /workspace

# 소스보다 변경이 적은 빌드 설정을 먼저 복사해 레이어 캐시를 최대한 활용한다.
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY member-domain/build.gradle ./member-domain/
COPY member-api/build.gradle ./member-api/

COPY member-domain/src ./member-domain/src
COPY member-api/src ./member-api/src
# processResources가 docs/flyway의 마이그레이션 SQL을 클래스패스로 복사하므로 함께 넣는다.
COPY docs/flyway ./docs/flyway

# 의존성은 BuildKit 캐시 마운트(/root/.gradle)에 남겨 재빌드 시 다시 받지 않는다.
# 테스트는 Testcontainers가 필요해 이미지 빌드에서는 제외한다.
# bootJar 결과물 중 plain jar(의존성 미포함)는 제외하고 실행 가능한 jar만 app.jar로 꺼낸다.
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :member-api:bootJar -x test --no-daemon \
    && cp "$(ls member-api/build/libs/*.jar | grep -v -- '-plain.jar')" app.jar


# member-api 실행 Stage
FROM eclipse-temurin:21.0.13_9-jre-noble

WORKDIR /app

# 컨테이너 안에서 root로 실행하지 않도록 전용 계정을 만든다.
RUN groupadd --system app && useradd --system --gid app app

COPY --from=builder /workspace/app.jar app.jar

USER app

# 애플리케이션 HTTP 포트 (application.yml의 server.port)
EXPOSE 8080

# 프로필(SPRING_PROFILES_ACTIVE)과 DB, Redis, JWT 값은 실행 환경(compose, k8s)에서 환경변수로 주입한다.
ENTRYPOINT ["java", "-jar", "app.jar"]
