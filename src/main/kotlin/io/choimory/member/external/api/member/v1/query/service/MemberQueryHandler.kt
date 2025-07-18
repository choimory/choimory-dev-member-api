package io.choimory.member.external.api.member.v1.query.service

import io.choimory.member.external.api.member.v1.query.domain.document.MemberDocument
import io.choimory.member.external.api.member.v1.query.domain.dto.MemberQueryDto
import io.choimory.member.external.api.member.v1.query.repository.MemberQueryClient
import io.choimory.member.external.api.member.v1.query.repository.MemberQueryRepository
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class MemberQueryHandler(
    private val passwordEncoder: PasswordEncoder,
    private val memberQueryRepository: MemberQueryRepository,
    private val memberQueryClient: MemberQueryClient,
){
    fun loginAndValid(email:String, password:String):MemberQueryDto{
        val member:MemberDocument = memberQueryRepository.findByEmail(email)
            .orElseThrow{ BadCredentialsException("아이디가 존재하지 않습니다") }

        // 비밀번호 확인
        if(!passwordEncoder.matches(password, member.password)){
            throw BadCredentialsException("비밀번호가 일치하지 않습니다")
        }

        return MemberQueryDto.from(member)
    }
}
