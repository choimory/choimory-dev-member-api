package dev.choimory.member.api.login.v1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.common.domain.response.CommonResponse;
import dev.choimory.member.api.login.v1.domain.response.LoginResponse;
import dev.choimory.member.api.token.v1.domain.TokenDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인 인증 성공 응답을 작성하는 핸들러입니다.
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper; // JSON 변환 객체

    /**
     * 인증 성공 응답을 JSON으로 작성합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param authentication 인증 결과
     * @throws IOException 응답 작성 실패 시 발생
     */
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        // HTTP 응답 기본값을 설정한다.
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // 토큰 details를 획득한다.
        TokenDetails details = authentication != null && authentication.getCredentials() instanceof TokenDetails tokenDetails
                ? tokenDetails
                : null;

        // TODO refresh, access 토큰 생성
        // TODO redis 등록

        // 공통 응답 형식으로 로그인 성공 내용을 작성한다.
        objectMapper.writeValue(
                response.getWriter(),
                new CommonResponse<>(
                        HttpStatus.OK.value(),
                        HttpStatus.OK.name(),
                        HttpStatus.OK.getReasonPhrase(),
                        new LoginResponse("access-token", "refresh-token")
                )
        );
    }
}
