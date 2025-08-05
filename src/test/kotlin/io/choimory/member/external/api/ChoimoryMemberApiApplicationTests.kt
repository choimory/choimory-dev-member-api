package io.choimory.member.external.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class ChoimoryMemberApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
