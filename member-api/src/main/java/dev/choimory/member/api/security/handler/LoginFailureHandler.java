package dev.choimory.member.api.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인에 실패하면 401 공통 응답을 내려주는 핸들러입니다.
 * 없는 이메일, 틀린 비밀번호, 로그인할 수 없는 상태의 회원을 구분하면 계정 정보가 노출되므로 모두 같은 응답을 내려줍니다.
 */
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 401 공통 응답을 작성합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param exception 인증 예외
     * @throws IOException 응답을 쓰지 못한 경우
     */
    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        securityResponseWriter.write(response, HttpStatus.UNAUTHORIZED);
    }
}
