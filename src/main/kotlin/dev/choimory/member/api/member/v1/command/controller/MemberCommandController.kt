package dev.choimory.member.api.member.v1.command.controller

import dev.choimory.member.api.member.v1.command.domain.request.CreateMemberRequest
import dev.choimory.member.api.member.v1.command.domain.request.VerifyMemberRequest
import dev.choimory.member.api.member.v1.command.domain.response.CreateMemberResponse
import dev.choimory.member.api.member.v1.command.domain.response.VerifyMemberResponse
import dev.choimory.member.api.member.v1.command.service.MemberCommandService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/member")
@Validated
class MemberCommandController(
    private val memberCommandService: MemberCommandService,
) {
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.OK)
    fun signup(
        @Valid payload: CreateMemberRequest,
    ): CreateMemberResponse {
        return memberCommandService.signup(payload)
    }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.CREATED)
    fun verify(
        @Valid payload: VerifyMemberRequest,
    ): VerifyMemberResponse {
        return memberCommandService.verify(payload)
    }
}
