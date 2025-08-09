package dev.choimory.member.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class ChoimoryDevMemberApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
