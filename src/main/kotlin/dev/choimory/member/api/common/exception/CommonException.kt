package dev.choimory.member.api.common.exception

import org.springframework.http.HttpStatus

class CommonException(
    val status: HttpStatus, // e.g. HttpStatus.OK
    val code: Int? = status.value(), // e.g. 200
    val codeName: String? = status.name, // e.g. OK
    val msg: String? = status.reasonPhrase, // e.g. 성공하였습니다
) : RuntimeException()
