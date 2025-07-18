package io.choimory.member.external.api.login

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
    val url: String,
    val authenticationManager: AuthenticationManager,
    val successHandler: AuthenticationSuccessHandler,
    val failureHandler: AuthenticationFailureHandler,
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
        return authenticationManager.authenticate(token)
    }
}
