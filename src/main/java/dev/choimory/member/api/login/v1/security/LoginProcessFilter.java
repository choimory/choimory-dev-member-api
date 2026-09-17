package dev.choimory.member.api.login.v1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.login.v1.domain.request.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 로그인 요청 payload를 Spring Security 인증 객체로 변환하는 필터입니다.
 */
public class LoginProcessFilter extends AbstractAuthenticationProcessingFilter {

    private final ObjectMapper objectMapper; // JSON 변환 객체

    /**
     * 로그인 처리 필터를 생성합니다.
     *
     * @param url 로그인 처리 URL
     * @param authenticationManager 인증 매니저
     * @param successHandler 인증 성공 핸들러
     * @param failureHandler 인증 실패 핸들러
     * @param objectMapper JSON 변환 객체
     */
    public LoginProcessFilter(
            String url,
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler,
            ObjectMapper objectMapper) {
        super(url, authenticationManager);
        this.objectMapper = objectMapper;
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
    }

    /**
     * HTTP 요청 payload에서 로그인 정보를 읽고 인증을 시도합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 인증 결과
     * @throws AuthenticationException 인증 실패 시 발생
     * @throws IOException 요청 payload 읽기 실패 시 발생
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException {
        // 요청 payload를 로그인 요청 객체로 변환한다.
        LoginRequest loginRequest = objectMapper.readValue(request.getInputStream(), LoginRequest.class);

        // Spring Security 인증 객체로 변환한다.
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());

        // Provider로 인증 처리를 위임한다.
        return getAuthenticationManager().authenticate(token);
    }
}
