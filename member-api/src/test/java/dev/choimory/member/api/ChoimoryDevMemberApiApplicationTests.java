package dev.choimory.member.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 애플리케이션 컨텍스트 로딩 테스트입니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("local")
class ChoimoryDevMemberApiApplicationTests {

    /**
     * 애플리케이션 컨텍스트가 로딩되는지 확인합니다.
     */
    @Test
    void contextLoads() {}
}
