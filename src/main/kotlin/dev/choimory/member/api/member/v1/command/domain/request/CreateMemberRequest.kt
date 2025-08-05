package dev.choimory.member.api.member.v1.command.domain.request

import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile

data class CreateMemberRequest(
    @NotBlank
    val email: String,
    @NotBlank
    val password: String,
    @NotBlank
    val nickname: String,
    val introduce: String?,
    val profile: MultipartFile?,
)
