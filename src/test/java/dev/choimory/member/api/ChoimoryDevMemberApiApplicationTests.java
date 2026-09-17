package dev.choimory.member.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Spring Boot 애플리케이션 컨텍스트 로딩 테스트입니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        properties = {
            "es-host=localhost",
            "es-port=9200",
            "es-user=",
            "es-password=",
            "redis-host=localhost",
            "redis-port=6379",
            "redis-database=0",
            "redis-user=",
            "redis-password="
        })
class ChoimoryDevMemberApiApplicationTests {

    /**
     * 애플리케이션 컨텍스트가 로딩되는지 확인합니다.
     */
    @Test
    void contextLoads() {}
}
