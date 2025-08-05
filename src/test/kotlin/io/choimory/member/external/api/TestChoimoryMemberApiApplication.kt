package io.choimory.member.external.api

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<ChoimoryMemberApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
