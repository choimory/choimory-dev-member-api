package dev.choimory.member.api.login.v1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.common.domain.response.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인 인증 실패 응답을 작성하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper; // JSON 변환 객체

    /**
     * 인증 실패 응답을 JSON으로 작성합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param exception 인증 예외
     * @throws IOException 응답 작성 실패 시 발생
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        // HTTP 응답 기본값을 설정한다.
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 공통 응답 형식으로 인증 실패 내용을 작성한다.
        objectMapper.writeValue(
                response.getWriter(),
                new CommonResponse<>(
                        HttpStatus.UNAUTHORIZED.value(),
                        HttpStatus.UNAUTHORIZED.name(),
                        exception != null ? exception.getMessage() : HttpStatus.UNAUTHORIZED.getReasonPhrase()
                )
        );
    }
}
