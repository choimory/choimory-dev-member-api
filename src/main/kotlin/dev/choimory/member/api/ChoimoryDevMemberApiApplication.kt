package dev.choimory.member.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class ChoimoryDevMemberApiApplication

fun main(args: Array<String>) {
    runApplication<ChoimoryDevMemberApiApplication>(*args)
}
