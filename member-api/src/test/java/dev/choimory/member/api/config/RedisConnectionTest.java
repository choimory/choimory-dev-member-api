package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 접속 설정(application-redis.yml) 값이 비어 있어도 접속되는지 확인하는 테스트입니다.
 * 다른 통합 테스트는 Testcontainers가 연결 정보를 덮어쓰므로 설정값 자체의 동작은 이 테스트에서 확인합니다.
 */
@Testcontainers
class RedisConnectionTest {

    @Container
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

    /**
     * username과 password가 빈 값(환경변수 미설정 시의 기본값)이어도 접속해 읽고 쓸 수 있어야 합니다.
     */
    @Test
    void blankUsernameAndPasswordConnect() {
        runner.withPropertyValues(
                        "spring.data.redis.host=" + REDIS.getHost(),
                        "spring.data.redis.port=" + REDIS.getMappedPort(6379),
                        "spring.data.redis.username=",
                        "spring.data.redis.password=")
                .run(context -> {
                    // 빈 값으로 접속해 값을 쓰고 읽는다.
                    StringRedisTemplate template = context.getBean(StringRedisTemplate.class);
                    template.opsForValue().set("redis-connection-test", "ok");
                    assertThat(template.opsForValue().get("redis-connection-test"))
                            .isEqualTo("ok");
                });
    }
}
