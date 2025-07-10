package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.repository.MemberQueryRepository
import org.springframework.stereotype.Component

@Component
class MemberQueryHandler(
    private val memberQueryRepository: MemberQueryRepository,
)
