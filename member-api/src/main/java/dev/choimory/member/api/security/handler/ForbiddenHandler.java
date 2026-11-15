package dev.choimory.member.api.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 되었으나 권한이 없는 요청을 403 공통 응답으로 변환하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class ForbiddenHandler implements AccessDeniedHandler {

    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 403 공통 응답을 작성합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param accessDeniedException 접근 거부 예외
     * @throws IOException 응답을 쓰지 못한 경우
     */
    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        securityResponseWriter.write(response, HttpStatus.FORBIDDEN);
    }
}
