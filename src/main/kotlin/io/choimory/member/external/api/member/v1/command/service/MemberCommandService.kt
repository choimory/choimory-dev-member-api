package io.choimory.member.external.api.member.v1.command.service

import org.springframework.stereotype.Service

@Service
class MemberCommandService(
    private val memberCommandHandler: MemberCommandHandler,
)
