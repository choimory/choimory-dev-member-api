package io.choimory.member.external.api.member.v1.command.service

import io.choimory.member.external.api.mail.v1.domain.dto.MailDto
import io.choimory.member.external.api.mail.v1.handler.MailHandler
import io.choimory.member.external.api.member.v1.command.domain.dto.MemberEntityDto
import io.choimory.member.external.api.member.v1.command.domain.entity.MemberEntity
import io.choimory.member.external.api.member.v1.command.domain.response.VerifyMemberResponse
import io.choimory.member.external.api.member.v1.command.repository.MemberCommandRepository
import org.springframework.data.redis.core.RedisTemplate
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
        ttl: Int,
        timeUnit: TimeUnit,
    ) {
        TODO("redis set with verify code")
    }

    fun sendEmailWithVerifyCode(member: MemberEntityDto) {
        TODO()
    }

    fun getWaitVerifyMember(
        uuid:String,
        email: String,
        verifyCode: Int,
    ): MemberEntityDto? {
        TODO("redis get with verify code")
    }

    fun saveVerifiedMember(member: MemberEntityDto): MemberEntity {
        TODO()
    }

    fun generateToken(member: MemberEntityDto): VerifyMemberResponse {
        TODO("generate access token, refresh token")
    }
}
