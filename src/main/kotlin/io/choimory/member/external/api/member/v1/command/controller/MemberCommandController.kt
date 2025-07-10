package io.choimory.member.external.api.member.v1.command.controller

import io.choimory.member.external.api.member.v1.command.service.MemberCommandHandler
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/member")
@Validated
class MemberCommandController(
    private val memberCommandHandler: MemberCommandHandler,
)
