package io.choimory.member.external.api.common.exception

import org.springframework.http.HttpStatus

class CommonException(
    val status: HttpStatus,
    val code: Int,
    val msg: String,
) : RuntimeException()
