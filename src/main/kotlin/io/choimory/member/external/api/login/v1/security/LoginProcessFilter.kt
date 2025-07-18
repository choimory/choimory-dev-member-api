package io.choimory.member.external.api.login.v1.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.choimory.member.external.api.login.domain.request.LoginRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

class LoginProcessFilter(
    url: String,
    authenticationManager: AuthenticationManager,
    successHandler: AuthenticationSuccessHandler,
    failureHandler: AuthenticationFailureHandler,
    val objectMapper: ObjectMapper,
) : AbstractAuthenticationProcessingFilter(
        url,
        authenticationManager,
    ) {
    init {
        setAuthenticationSuccessHandler(successHandler)
        setAuthenticationFailureHandler(failureHandler)
    }

    override fun attemptAuthentication(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
    ): Authentication {
        // 요청 playload를 객체로 변환
        val loginRequest: LoginRequest = objectMapper.readValue(request?.inputStream, LoginRequest::class.java)

        // security 제공 객체로 변환
        val token: UsernamePasswordAuthenticationToken = UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)

        // step to provider
        return this.authenticationManager.authenticate(token)
    }
}
