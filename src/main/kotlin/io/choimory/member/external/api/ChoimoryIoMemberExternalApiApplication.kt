package io.choimory.member.external.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class ChoimoryIoMemberExternalApiApplication

fun main(args: Array<String>) {
    runApplication<ChoimoryIoMemberExternalApiApplication>(*args)
}
