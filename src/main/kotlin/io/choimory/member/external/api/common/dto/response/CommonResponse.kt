package io.choimory.member.external.api.common.dto.response

data class CommonResponse<T>(
    val code: Int,
    val message: String,
    val data: T?=null,
)
