package dev.choimory.member.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.jwt.v1.command.handler.JwtCommandHandler;
import dev.choimory.member.api.jwt.v1.command.payload.dto.JwtProperties;
import dev.choimory.member.api.jwt.v1.command.util.JwtProvider;
import dev.choimory.member.api.security.filter.JwtAuthenticationFilter;
import dev.choimory.member.api.security.filter.LoginFilter;
import dev.choimory.member.api.security.filter.LogoutFilter;
import dev.choimory.member.api.security.handler.ForbiddenHandler;
import dev.choimory.member.api.security.handler.LoginFailureHandler;
import dev.choimory.member.api.security.handler.LoginSuccessHandler;
import dev.choimory.member.api.security.handler.SecurityResponseWriter;
import dev.choimory.member.api.security.handler.UnauthorizedHandler;
import dev.choimory.member.api.security.user.MemberUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정입니다.
 * 토큰 기반 API이므로 세션과 CSRF, 폼 로그인, HTTP Basic을 사용하지 않습니다.
 * JWT 인증 필터와 로그인, 로그아웃 필터를 연결합니다. 토큰 재발급은 member 컨트롤러가 처리합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UnauthorizedHandler unauthorizedHandler; // 인증 실패(401) 응답
    private final ForbiddenHandler forbiddenHandler; // 권한 없음(403) 응답
    private final JwtProvider jwtProvider; // JWT 검증
    private final JwtCommandHandler jwtCommandHandler; // 블랙리스트 확인, 로그아웃 처리
    private final MemberUserDetailsService memberUserDetailsService; // 로그인 인증용 회원 조회
    private final PasswordEncoder passwordEncoder; // 비밀번호 검증
    private final LoginSuccessHandler loginSuccessHandler; // 로그인 성공(토큰 발급) 응답
    private final LoginFailureHandler loginFailureHandler; // 로그인 실패(401) 응답
    private final SecurityResponseWriter securityResponseWriter; // 공통 응답 작성
    private final ObjectMapper objectMapper; // JSON 변환

    /**
     * 로그인 인증 관리자를 구성합니다. 이메일로 회원을 조회하고 비밀번호를 검증합니다.
     * 존재하지 않는 이메일은 비밀번호 오류와 같은 인증 실패로 처리되어 계정 존재 여부가 노출되지 않습니다.
     *
     * @return 인증 관리자
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(memberUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /**
     * 보안 필터 체인을 구성합니다.
     *
     * @param http HTTP 보안 설정
     * @return 보안 필터 체인
     * @throws Exception 설정 중 발생
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 세션을 만들지 않는 토큰 기반 API이므로 CSRF, 폼 로그인, HTTP Basic, 기본 로그아웃을 끈다.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO 클라이언트가 정해지면 CORS를 설정한다.
                // 회원가입, 로그인, 토큰 재발급만 인증 없이 허용하고 나머지는 인증을 요구한다.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/member/join",
                                LoginFilter.LOGIN_PATH,
                                LogoutFilter.LOGOUT_PATH,
                                "/v1/member/reissue")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // 필터 단계의 인증 실패와 권한 없음을 공통 응답 JSON으로 내려준다.
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedHandler).accessDeniedHandler(forbiddenHandler))
                // Access Token을 검증해 인증 정보를 채운다. 빈으로 등록하면 서블릿 필터로 한 번 더 등록되어 직접 생성한다.
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider, jwtCommandHandler),
                        UsernamePasswordAuthenticationFilter.class)
                // 로그인 요청(POST /v1/login)을 처리한다. JWT 필터와 같은 이유로 직접 생성한다.
                .addFilterAfter(
                        new LoginFilter(
                                authenticationManager(),
                                loginSuccessHandler,
                                loginFailureHandler,
                                objectMapper,
                                securityResponseWriter),
                        JwtAuthenticationFilter.class)
                // 로그아웃 요청(POST /v1/logout)을 처리한다. 인증 기준이 Refresh Token이라 로그인 필터 뒤에 둔다.
                .addFilterAfter(
                        new LogoutFilter(jwtCommandHandler, objectMapper, securityResponseWriter), LoginFilter.class);
        return http.build();
    }
}
