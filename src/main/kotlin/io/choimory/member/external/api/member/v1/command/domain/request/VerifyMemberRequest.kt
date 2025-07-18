package io.choimory.member.external.api.member.v1.command.domain.request

data class VerifyMemberRequest(
    val email: String,
    val verifyCode: Int,
)
