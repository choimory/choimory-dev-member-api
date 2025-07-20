package io.choimory.member.external.api.login.v1.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.choimory.member.external.api.common.domain.response.CommonResponse
import io.choimory.member.external.api.login.domain.response.LoginResponse
import io.choimory.member.external.api.token.v1.domain.TokenDetails
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class LoginSuccessHandler(
    val objectMapper: ObjectMapper,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authentication: Authentication?,
    ) {
        // http 응답 객체 설정하여 종료
        response!!.status = HttpServletResponse.SC_OK
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        // 토큰 details 획득
        val details: TokenDetails = authentication?.credentials as TokenDetails

        // TODO refresh, access 토큰 생성

        // TODO redis 등록

        // 응답
        objectMapper.writeValue(
            response.writer,
            CommonResponse(
                HttpStatus.OK.value(),
                HttpStatus.OK.reasonPhrase,
                LoginResponse("access-token", "refresh-token"),
            ),
        )
    }
}
