package io.choimory.member.external.api.member.v1.command.service

import com.github.f4b6a3.uuid.UuidCreator
import io.choimory.member.external.api.member.v1.command.domain.dto.MemberEntityDto
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
    fun signup(request: CreateMemberRequest): CreateMemberResponse {
        // 비밀번호 암호화
        val encodedPassword: String = memberCommandHandler.encodePassword(request.password)

        // UUID, 인증코드 생성
        val uuid: String = UuidCreator.getTimeOrderedEpoch().toString()
        val verifyCode: String = memberCommandHandler.generateVerifyCode()

        // Redis set
        val member: MemberEntityDto =
            MemberEntityDto(id = uuid, email = request.email, password = encodedPassword, nickname = request.nickname, introduce = request.introduce)
        memberCommandHandler.setWaitVerifyMember(member, verifyCode, 3, TimeUnit.MINUTES)

        // 이메일 발송
        memberCommandHandler.sendEmailWithVerifyCode(member)

        return CreateMemberResponse()
    }

    @Transactional
    fun verify(payload: VerifyMemberRequest): VerifyMemberResponse {
        // Redis get
        val member: MemberEntityDto =
            requireNotNull(memberCommandHandler.getWaitVerifyMember(payload.uuid, payload.email, payload.verifyCode)) { throw IllegalArgumentException() }

        // Save Member
        memberCommandHandler.saveVerifiedMember(member)

        // 토큰 발행
        val response: VerifyMemberResponse = memberCommandHandler.generateToken(member)

        return response
    }
}
