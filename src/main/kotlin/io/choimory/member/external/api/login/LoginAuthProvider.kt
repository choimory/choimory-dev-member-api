package io.choimory.member.external.api.login

import io.choimory.member.external.api.common.exception.CommonException
import io.choimory.member.external.api.member.v1.query.domain.dto.MemberQueryDto
import io.choimory.member.external.api.member.v1.query.service.MemberQueryService
import io.choimory.member.external.api.token.domain.TokenDetails
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class LoginAuthProvider(
    val memberQueryService: MemberQueryService,
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication?): Authentication {
        // security 표준 객체에서 id, password 획득
        val email: String? = authentication?.name
        val password: String? = authentication?.credentials?.toString()

        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            throw CommonException(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.reasonPhrase)
        }

        // 별도 비즈니스 로직에서 검증
        val member: MemberQueryDto = memberQueryService.login(email, password)

        // 토큰 Claim에 사용할 객체로 변경
        val details: TokenDetails = TokenDetails.of(member)

        // step to success handler
        return UsernamePasswordAuthenticationToken(email, details)
    }

    override fun supports(authentication: Class<*>?): Boolean {
        return (UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication))
    }
}
