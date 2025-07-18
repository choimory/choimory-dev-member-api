package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.domain.document.MemberDocument
import io.choimory.member.external.api.member.v1.query.domain.dto.MemberQueryDto
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class MemberQueryService(
    private val memberQueryHandler: MemberQueryHandler,
) {
    fun login(
        email: String,
        password: String,
    ): MemberQueryDto {
        // 검증, 조회
        val member:MemberQueryDto = memberQueryHandler.loginAndValid(email, password)

        // 응답
        return member
    }
}
