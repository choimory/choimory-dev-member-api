package dev.choimory.member.api.common.domain.response

data class CommonResponse<T>(
    val code: Int, // e.g. 200
    val name: String, // e.g. OK
    val message: String?, // e.g. 성공하였습니다
    val data: T? = null,
)
