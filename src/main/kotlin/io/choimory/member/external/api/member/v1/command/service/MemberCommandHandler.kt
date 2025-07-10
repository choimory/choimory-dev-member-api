package io.choimory.member.external.api.member.v1.command.service

import org.springframework.stereotype.Component

@Component
class MemberCommandHandler(
    private val memberCommandService: MemberCommandService,
)
