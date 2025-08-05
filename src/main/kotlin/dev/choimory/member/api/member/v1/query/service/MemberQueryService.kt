package dev.choimory.member.api.member.v1.query.service

import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryHandler: dev.choimory.member.api.member.v1.query.service.MemberQueryHandler,
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
