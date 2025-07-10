package io.choimory.member.external.api.member.v1.query.controller

import io.choimory.member.external.api.member.v1.query.service.MemberQueryHandler
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/member")
@Validated
class MemberQueryController(
    private val memberQueryHandler: MemberQueryHandler,
)
