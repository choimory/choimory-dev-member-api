package dev.choimory.member.api.security.handler;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.security.filter.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 요청을 401 공통 응답으로 변환하는 진입점입니다.
 * 인증 실패 사유(계정 존재 여부 등)는 노출하지 않으며, 토큰 오류는 만료와 잘못된 토큰으로만 구분해 응답합니다.
 */
@Component
@RequiredArgsConstructor
public class UnauthorizedHandler implements AuthenticationEntryPoint {

    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 401 공통 응답을 작성합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authException 인증 예외
     * @throws IOException 응답을 쓰지 못한 경우
     */
    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        // JWT 필터가 토큰 오류(만료, 잘못된 토큰)를 담아 두었다면 그 종류로 응답하고, 없으면 일반 401로 응답한다.
        if (request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE) instanceof CommonException tokenError) {
            securityResponseWriter.write(response, tokenError);
            return;
        }
        securityResponseWriter.write(response, HttpStatus.UNAUTHORIZED);
    }
}
