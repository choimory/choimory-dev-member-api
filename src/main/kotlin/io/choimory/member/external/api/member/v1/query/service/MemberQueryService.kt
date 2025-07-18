package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.domain.dto.MemberQueryDto
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryHandler: MemberQueryHandler,
) {
    fun login(
        id: String,
        password: String,
    ): MemberQueryDto {
        TODO()
    }
}
