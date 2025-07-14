package io.choimory.member.external.api.member.v1.command.service

import io.choimory.member.external.api.member.v1.command.domain.dto.MemberDto
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
        member: MemberDto,
    ) {
        TODO("redis set with verify code")
    }

    fun sendEmailWithVerifyCode(member: MemberDto) {
        TODO()
    }

    fun getWaitVerifyMember(
        verifyCode: Int,
        email: String,
    ): MemberDto? {
        TODO("redis get with verify code")
    }

    fun saveVerifiedMember(member: MemberDto): MemberEntity {
        TODO()
    }

    fun generateToken(member: MemberDto): VerifyMemberResponse {
        TODO("generate access token, refresh token")
    }
}
