package io.choimory.member.external.api.member.v1.command.domain.dto

data class MemberEntityDto(
    val id: String? = null,
    val email: String? = null,
    val nickname: String? = null,
    val password: String? = null,
    val introduce: String? = null,
)
