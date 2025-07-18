package io.choimory.member.external.api.login.v1.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.choimory.member.external.api.common.domain.response.CommonResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class LoginFailureHandler(val objectMapper: ObjectMapper) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        exception: AuthenticationException?,
    ) {
        // http 응답 객체 설정하여 종료
        response!!.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        response.writer.write(
            objectMapper.writeValueAsString(
                CommonResponse<Nothing>(
                    HttpStatus.UNAUTHORIZED.value(),
                    exception?.message ?: HttpStatus.UNAUTHORIZED.reasonPhrase,
                ),
            ),
        )
    }
}
