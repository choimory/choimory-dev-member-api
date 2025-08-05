package dev.choimory.member.api.member.v1.query.controller

import dev.choimory.member.api.member.v1.query.service.MemberQueryService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/member")
@Validated
class MemberQueryController(
    private val memberQueryService: MemberQueryService,
)
