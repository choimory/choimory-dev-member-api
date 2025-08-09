package dev.choimory.member.api

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<ChoimoryDevMemberApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
