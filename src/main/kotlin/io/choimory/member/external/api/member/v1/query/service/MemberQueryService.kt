package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.repository.MemberQueryRepository
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryRepository: MemberQueryRepository,
)
