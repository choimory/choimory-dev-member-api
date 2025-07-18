package io.choimory.member.external.api.member.v1.command.domain.request

import jakarta.validation.constraints.NotBlank

data class CreateMemberRequest(
    @NotBlank
    val password: String,
)
