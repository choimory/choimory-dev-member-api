package dev.choimory.member.api.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.auth.v1.command.payload.request.LoginRequest;
import dev.choimory.member.api.security.handler.SecurityResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * 로그인 필터입니다. {@code POST /v1/auth/login}의 JSON 본문({@code {email, password}})을 읽어 인증합니다.
 * 성공과 실패 처리는 각각 {@link AuthenticationSuccessHandler}, {@link AuthenticationFailureHandler}가 합니다.
 * 요청 본문이 JSON이 아니거나 이메일, 비밀번호가 비어 있으면 인증을 시도하지 않고 400으로 응답합니다.
 * 스프링 빈으로 등록하면 서블릿 필터로 한 번 더 등록되므로 SecurityConfig에서 직접 생성합니다.
 */
public class LoginFilter extends AbstractAuthenticationProcessingFilter {

    /** 로그인 주소입니다. */
    public static final String LOGIN_PATH = "/v1/auth/login";

    private final ObjectMapper objectMapper; // JSON 변환
    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 로그인 필터를 생성합니다.
     *
     * @param authenticationManager 인증 관리자
     * @param successHandler 로그인 성공 핸들러
     * @param failureHandler 로그인 실패 핸들러
     * @param objectMapper JSON 변환
     * @param securityResponseWriter 공통 응답 작성
     */
    public LoginFilter(
            AuthenticationManager authenticationManager,
            AuthenticationSuccessHandler successHandler,
            AuthenticationFailureHandler failureHandler,
            ObjectMapper objectMapper,
            SecurityResponseWriter securityResponseWriter) {
        super(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGIN_PATH), authenticationManager);
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        this.objectMapper = objectMapper;
        this.securityResponseWriter = securityResponseWriter;
    }

    /**
     * 요청 본문의 이메일과 비밀번호로 인증을 시도합니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @return 인증 결과, 요청 본문이 잘못되어 400으로 응답했다면 null
     * @throws IOException 응답을 쓰지 못한 경우
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // 요청 본문을 읽는다. JSON이 아니거나 이메일, 비밀번호가 비어 있으면 400으로 응답하고 끝낸다.
        LoginRequest loginRequest = readLoginRequest(request);
        if (loginRequest == null
                || loginRequest.email() == null
                || loginRequest.email().isBlank()
                || loginRequest.password() == null
                || loginRequest.password().isBlank()) {
            securityResponseWriter.write(response, HttpStatus.BAD_REQUEST);
            return null;
        }

        // 이메일과 비밀번호로 인증한다.
        return getAuthenticationManager()
                .authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.email(), loginRequest.password()));
    }

    /**
     * 요청 본문을 로그인 요청으로 읽습니다.
     *
     * @param request HTTP 요청
     * @return 로그인 요청, JSON이 아니거나 읽지 못하면 null
     */
    private LoginRequest readLoginRequest(HttpServletRequest request) {
        try {
            return objectMapper.readValue(request.getInputStream(), LoginRequest.class);
        } catch (IOException e) {
            return null;
        }
    }
}
