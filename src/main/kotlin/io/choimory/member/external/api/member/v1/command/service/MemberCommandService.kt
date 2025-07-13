package io.choimory.member.external.api.member.v1.command.service

import io.choimory.member.external.api.member.v1.command.domain.request.CreateMemberRequest
import io.choimory.member.external.api.member.v1.command.domain.request.VerifyMemberRequest
import io.choimory.member.external.api.member.v1.command.domain.response.CreateMemberResponse
import io.choimory.member.external.api.member.v1.command.domain.response.VerifyMemberResponse
import org.springframework.stereotype.Service

@Service
class MemberCommandService(
    private val memberCommandHandler: MemberCommandHandler,
) {
    fun signup(payload: CreateMemberRequest): CreateMemberResponse {
        // 비밀번호 암호화
        // 인증코드 생성
        // Redis set
        // 이메일 발송

        return CreateMemberResponse()
    }

    fun verify(payload: VerifyMemberRequest): VerifyMemberResponse {
        // Redis get
        // 인증코드 검증
        // Save Member
        // 토큰 발행

        return VerifyMemberResponse()
    }
}
