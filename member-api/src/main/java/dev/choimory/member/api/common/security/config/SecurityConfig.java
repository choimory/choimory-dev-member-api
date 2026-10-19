package dev.choimory.member.api.common.security.config;

import dev.choimory.member.api.common.security.handler.CommonAccessDeniedHandler;
import dev.choimory.member.api.common.security.handler.CommonAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정입니다.
 * 토큰 기반 API이므로 세션과 CSRF, 폼 로그인, HTTP Basic을 사용하지 않습니다.
 * JWT 인증 필터와 로그인, 로그아웃 필터는 이후 단계에서 추가합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CommonAuthenticationEntryPoint authenticationEntryPoint; // 인증 실패(401) 응답
    private final CommonAccessDeniedHandler accessDeniedHandler; // 권한 없음(403) 응답

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
                // 회원가입만 인증 없이 허용하고 나머지는 인증을 요구한다.
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/v1/member/join")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // 필터 단계의 인증 실패와 권한 없음을 공통 응답 JSON으로 내려준다.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }
}
