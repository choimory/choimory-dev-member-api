package dev.choimory.member.api.security.filter;

import dev.choimory.member.api.common.exception.CommonException;
import dev.choimory.member.api.jwt.v1.command.handler.JwtCommandHandler;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtClaims;
import dev.choimory.member.api.jwt.v1.command.payload.type.TokenType;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import dev.choimory.member.api.security.user.AuthenticatedMember;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청의 Access Token을 검증해 인증 정보를 채우는 필터입니다.
 * 토큰의 클레임만으로 인증하므로 요청마다 DB를 조회하지 않고, 로그아웃한 토큰인지 Redis의 블랙리스트만 확인합니다.
 * 토큰 오류가 있어도 여기서 응답하지 않고 요청 속성에 담아 둔 채 다음으로 넘깁니다.
 * 인증이 필요한 주소라면 진입점이 오류 종류(만료, 잘못된 토큰)를 담아 401로 응답하고,
 * 인증이 필요 없는 주소(회원가입 등)는 토큰이 잘못되어도 그대로 처리됩니다.
 * 스프링 빈으로 등록하면 서블릿 필터로 한 번 더 등록되므로 SecurityConfig에서 직접 생성합니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** 토큰 검증 실패 예외를 담아 두는 요청 속성 이름입니다. */
    public static final String ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".error";

    /** 검증을 통과한 Access Token의 클레임을 담아 두는 요청 속성 이름입니다. 로그아웃 필터가 블랙리스트에 올릴 때 사용합니다. */
    public static final String ACCESS_CLAIMS_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".claims";

    private static final String BEARER_PREFIX = "Bearer "; // Authorization 헤더 접두어

    private final JwtProvider jwtProvider; // JWT 검증
    private final JwtCommandHandler jwtCommandHandler; // 블랙리스트 확인

    /**
     * Authorization 헤더의 Access Token을 검증해 인증 정보를 채웁니다.
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
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Bearer 토큰이 있을 때만 검증한다. 없으면 인증되지 않은 요청으로 넘긴다.
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                JwtClaims claims = jwtProvider.parse(
                        header.substring(BEARER_PREFIX.length()).trim(), TokenType.ACCESS);

                // 로그아웃한 토큰(블랙리스트)이면 잘못된 토큰으로 처리한다.
                if (jwtCommandHandler.isBlacklisted(claims.jti())) {
                    throw JwtProvider.invalidToken();
                }

                // 토큰의 클레임으로 인증 정보를 채운다.
                AuthenticatedMember principal = AuthenticatedMember.from(claims);
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
                SecurityContextHolder.setContext(context);
                request.setAttribute(ACCESS_CLAIMS_ATTRIBUTE, claims);
            } catch (CommonException e) {
                // 인증 정보를 비우고 오류를 요청 속성에 담아 진입점이 응답하게 한다.
                SecurityContextHolder.clearContext();
                request.setAttribute(ERROR_ATTRIBUTE, e);
            } catch (DataAccessException e) {
                // Redis 장애로 블랙리스트를 확인하지 못하면 인증하지 않는다. (fail-closed)
                // 인증이 필요한 주소는 진입점이 500으로 응답하고, 인증이 필요 없는 주소는 그대로 처리된다.
                log.error("블랙리스트 확인 중 Redis 오류가 발생했습니다.", e);
                SecurityContextHolder.clearContext();
                request.setAttribute(
                        ERROR_ATTRIBUTE,
                        new CommonException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()));
            }
        }

        filterChain.doFilter(request, response);
    }
}
