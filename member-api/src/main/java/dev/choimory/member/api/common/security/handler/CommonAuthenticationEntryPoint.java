package dev.choimory.member.api.common.security.handler;

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
 * 인증 실패 사유(계정 존재 여부 등)가 노출되면 안 되어 응답에 담지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class CommonAuthenticationEntryPoint implements AuthenticationEntryPoint {

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
        securityResponseWriter.write(response, HttpStatus.UNAUTHORIZED);
    }
}
