package dev.choimory.member.api.member.v1.command.domain.request

data class VerifyMemberRequest(
    val email: String,
    val uuid: String,
    val verifyCode: Int,
)
