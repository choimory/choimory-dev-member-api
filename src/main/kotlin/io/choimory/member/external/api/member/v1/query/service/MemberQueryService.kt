package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.domain.dto.MemberDocumentDto
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryHandler: MemberQueryHandler,
) {
    fun login(
        email: String,
        password: String,
    ): MemberDocumentDto {
        // 검증, 조회
        val member: MemberDocumentDto = memberQueryHandler.loginAndValid(email, password)

        // 응답
        return member
    }
}
