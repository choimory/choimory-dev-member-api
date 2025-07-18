package io.choimory.member.external.api.member.v1.command.service

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
) {
    fun encodePassword(password: String): String {
        return passwordEncoder.encode(password)
    }

    fun generateVerifyCode(): Int {
        TODO()
    }

    fun setWaitVerifyMember(
        verifyCode: Int,
        ttl: Int,
        timeUnit: TimeUnit,
        member: MemberEntityDto,
    ) {
        TODO("redis set with verify code")
    }

    fun sendEmailWithVerifyCode(member: MemberEntityDto) {
        TODO()
    }

    fun getWaitVerifyMember(
        verifyCode: Int,
        email: String,
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
