package io.choimory.member.external.api.member.v1.command.domain.dto

data class MemberCommandDto(
    val id: Long? = null,
    val email: String? = null,
    val nickname: String? = null,
    val password: String? = null,
    val introduce: String? = null,
)
