package io.choimory.member.external.api.member.v1.query.service

import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryHandler: MemberQueryHandler,
)
