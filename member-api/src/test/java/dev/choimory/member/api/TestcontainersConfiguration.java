package dev.choimory.member.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트용 Testcontainers 설정입니다. PostgreSQL과 Redis 컨테이너를 띄웁니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * PostgreSQL 테스트 컨테이너 Bean을 생성합니다.
     *
     * @return PostgreSQL 테스트 컨테이너
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    }

    /**
     * Redis 테스트 컨테이너 Bean을 생성합니다. Refresh Token 저장과 Access Token 블랙리스트 테스트에 사용합니다.
     *
     * @return Redis 테스트 컨테이너
     */
    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
    }
}
