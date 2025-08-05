package dev.choimory.member.api.member.v1.query.service

import dev.choimory.member.api.member.v1.query.domain.document.MemberDocument
import dev.choimory.member.api.member.v1.query.domain.dto.MemberDocumentDto
import dev.choimory.member.api.member.v1.query.repository.MemberQueryClient
import dev.choimory.member.api.member.v1.query.repository.MemberQueryRepository
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class MemberQueryHandler(
    private val passwordEncoder: PasswordEncoder,
    private val memberQueryRepository: MemberQueryRepository,
    private val memberQueryClient: MemberQueryClient,
) {
    fun loginAndValid(
        email: String,
        password: String,
    ): MemberDocumentDto {
        val member: MemberDocument =
            memberQueryRepository.findByEmail(email)
                .orElseThrow { BadCredentialsException("아이디가 존재하지 않습니다") }

        // 비밀번호 확인
        if (!passwordEncoder.matches(password, member.password)) {
            throw BadCredentialsException("비밀번호가 일치하지 않습니다")
        }

        return MemberDocumentDto.from(member)
    }
}
