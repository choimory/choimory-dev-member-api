package io.choimory.member.external.api.member.v1.command.service

import io.choimory.member.external.api.member.v1.command.domain.dto.MemberDto
import io.choimory.member.external.api.member.v1.command.domain.request.CreateMemberRequest
import io.choimory.member.external.api.member.v1.command.domain.request.VerifyMemberRequest
import io.choimory.member.external.api.member.v1.command.domain.response.CreateMemberResponse
import io.choimory.member.external.api.member.v1.command.domain.response.VerifyMemberResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.TimeUnit

@Service
@Transactional(readOnly = true)
class MemberCommandService(
    private val memberCommandHandler: MemberCommandHandler,
) {
    fun signup(payload: CreateMemberRequest): CreateMemberResponse {
        // 비밀번호 암호화
        val encodedPassword: String = memberCommandHandler.encodePassword(payload.password)
        val member: MemberDto = MemberDto()

        // 인증코드 생성
        val verifyCode: Int = memberCommandHandler.generateVerifyCode()

        // Redis set
        memberCommandHandler.setWaitVerifyMember(verifyCode, 3, TimeUnit.MINUTES, member)

        // 이메일 발송
        memberCommandHandler.sendEmailWithVerifyCode(member)

        return CreateMemberResponse()
    }

    @Transactional
    fun verify(payload: VerifyMemberRequest): VerifyMemberResponse {
        // Redis get
        val member: MemberDto =
            requireNotNull(memberCommandHandler.getWaitVerifyMember(payload.verifyCode, payload.email)) { throw IllegalArgumentException() }

        // Save Member
        memberCommandHandler.saveVerifiedMember(member)

        // 토큰 발행
        val response: VerifyMemberResponse = memberCommandHandler.generateToken(member)

        return response
    }
}
