package dev.choimory.member.api.member.v1.command.service

import dev.choimory.member.api.common.util.TimeUnitUtil
import dev.choimory.member.api.mail.v1.handler.MailHandler
import dev.choimory.member.api.member.v1.command.domain.dto.MemberEntityDto
import dev.choimory.member.api.member.v1.command.domain.entity.MemberEntity
import dev.choimory.member.api.member.v1.command.domain.response.VerifyMemberResponse
import dev.choimory.member.api.member.v1.command.repository.MemberCommandRepository
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class MemberCommandHandler(
    private val memberCommandRepository: MemberCommandRepository,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val passwordEncoder: PasswordEncoder,
    private val mailHandler: MailHandler,
) {
    fun encodePassword(password: String): String {
        return passwordEncoder.encode(password)
    }

    fun generateVerifyCode(): String {
        return (100000..999999).random().toString()
    }

    fun setWaitVerifyMember(
        member: MemberEntityDto,
        verifyCode: String,
        ttl: Long,
        timeUnit: TimeUnit,
    ) {
        redisTemplate.opsForValue().set("${member.id}:$verifyCode", member, ttl, timeUnit)
    }

    /*fun sendEmailWithVerifyCode(
        email: String,
        verifyCode: String,
        minute: Int,
        unit: TimeUnit,
    ): SimpleMailMessage {
        val result: SimpleMailMessage =
            mailHandler.sendMail(
                listOf(email),
                null,
                "[choimory-io] 회원가입 인증코드 안내입니다",
                "[$verifyCode]를 ${minute}${TimeUnitUtil.toKorean(unit)}안에 입력해주세요",
            )
        return result
    }*/

    fun getWaitVerifyMember(
        uuid: String,
        email: String,
        verifyCode: Int,
    ): MemberEntityDto? {
        val member = redisTemplate.opsForValue().get("$uuid:$verifyCode") as? MemberEntityDto
        return member
    }

    fun saveVerifiedMember(member: MemberEntityDto): MemberEntityDto {
        val memberEntity: MemberEntity = MemberEntityDto.toEntity(member)
        val result: MemberEntity = memberCommandRepository.save(memberEntity)
        return MemberEntityDto.toDto(result)
    }

    fun generateToken(member: MemberEntityDto): VerifyMemberResponse {
        return VerifyMemberResponse("access", "refresh")
        TODO("generate access token, refresh token")
    }
}
