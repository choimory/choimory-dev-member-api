package io.choimory.member.external.api.member.v1.command.service

import io.choimory.member.external.api.member.v1.command.repository.MemberCommandRepository
import org.springframework.stereotype.Component

@Component
class MemberCommandHandler(
    private val memberCommandRepository: MemberCommandRepository,
)
