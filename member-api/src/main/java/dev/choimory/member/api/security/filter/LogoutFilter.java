package dev.choimory.member.api.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.jwt.v1.command.handler.JwtCommandHandler;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.security.handler.SecurityResponseWriter;
import dev.choimory.member.api.security.payload.request.LogoutRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 로그아웃 필터입니다. {@code POST /v1/logout}의 JSON 본문({@code {refreshToken}})을 읽어 Refresh Token을 폐기합니다.
 * 함께 온 유효한 Access Token(Authorization 헤더)이 같은 회원의 것이면 블랙리스트에 올려 만료 전에도 쓰지 못하게 합니다.
 * 인증 기준은 Refresh Token이므로 Access Token이 없거나 이미 만료되었어도 로그아웃할 수 있습니다.
 * Spring Security의 {@code org.springframework.security.web.authentication.logout.LogoutFilter}와 다른 클래스이며,
 * {@code logout()} 설정은 사용하지 않습니다. 스프링 빈으로 등록하면 서블릿 필터로 한 번 더 등록되므로
 * SecurityConfig에서 직접 생성합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class LogoutFilter extends OncePerRequestFilter {

    /** 로그아웃 주소입니다. */
    public static final String LOGOUT_PATH = "/v1/logout";

    private final RequestMatcher logoutMatcher =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, LOGOUT_PATH); // 로그아웃 요청 판별

    private final JwtCommandHandler jwtCommandHandler; // JWT 명령 핸들러
    private final ObjectMapper objectMapper; // JSON 변환
    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성

    /**
     * 로그아웃 요청이면 토큰을 폐기하고 응답합니다. 그 외 요청은 다음 필터로 넘깁니다.
     *
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 필터 처리 중 발생
     * @throws IOException 필터 처리 중 발생
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!logoutMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청 본문을 읽는다. JSON이 아니거나 Refresh Token이 비어 있으면 400으로 응답하고 끝낸다.
        LogoutRequest logoutRequest = readLogoutRequest(request);
        if (logoutRequest == null
                || logoutRequest.refreshToken() == null
                || logoutRequest.refreshToken().isBlank()) {
            securityResponseWriter.write(response, HttpStatus.BAD_REQUEST);
            return;
        }

        try {
            // JWT 필터가 검증을 마친 Access Token의 클레임이 있으면 함께 넘긴다.
            JwtClaims accessClaims = (JwtClaims) request.getAttribute(JwtAuthenticationFilter.ACCESS_CLAIMS_ATTRIBUTE);
            jwtCommandHandler.logout(logoutRequest.refreshToken(), accessClaims);
            securityResponseWriter.write(response, HttpStatus.OK);
        } catch (CommonException e) {
            // 만료(TOKEN_EXPIRED), 잘못된 Refresh Token(INVALID_TOKEN)은 401로 응답한다.
            securityResponseWriter.write(response, e);
        } catch (DataAccessException e) {
            // Redis 장애로 토큰을 폐기하지 못했으므로 성공으로 응답하지 않는다.
            log.error("로그아웃 중 Redis 오류가 발생했습니다.", e);
            securityResponseWriter.write(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 요청 본문을 로그아웃 요청으로 읽습니다.
     *
     * @param request HTTP 요청
     * @return 로그아웃 요청, JSON이 아니거나 읽지 못하면 null
     */
    private LogoutRequest readLogoutRequest(HttpServletRequest request) {
        try {
            return objectMapper.readValue(request.getInputStream(), LogoutRequest.class);
        } catch (IOException e) {
            return null;
        }
    }
}
