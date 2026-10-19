package dev.choimory.member.api.security.handler;

import dev.choimory.member.api.auth.v1.command.payload.response.LoginResponse;
import dev.choimory.member.api.auth.v1.command.service.AuthCommandService;
import dev.choimory.member.api.security.user.AuthenticatedMember;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인에 성공하면 토큰을 발급해 200 공통 응답으로 내려주는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthCommandService authCommandService; // 인증 토큰 명령 서비스
    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 인증된 회원의 토큰을 발급해 응답합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication 인증 결과
     * @throws IOException 응답을 쓰지 못한 경우
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        // 인증된 회원의 토큰을 발급한다.
        AuthenticatedMember member = (AuthenticatedMember) authentication.getPrincipal();
        LoginResponse loginResponse = authCommandService.issueTokens(member.getId(), member.authorityCodes());

        // 토큰을 200 공통 응답으로 내려준다.
        securityResponseWriter.write(response, HttpStatus.OK, loginResponse);
    }
}
