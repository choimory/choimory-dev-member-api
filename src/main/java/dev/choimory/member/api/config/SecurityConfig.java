package dev.choimory.member.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.choimory.member.api.login.v1.security.LoginAuthProvider;
import dev.choimory.member.api.login.v1.security.LoginFailureHandler;
import dev.choimory.member.api.login.v1.security.LoginProcessFilter;
import dev.choimory.member.api.login.v1.security.LoginSuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정입니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper; // JSON 변환 객체
    private final LoginAuthProvider loginAuthProvider; // 로그인 인증 Provider
    private final LoginSuccessHandler loginSuccessHandler; // 로그인 성공 핸들러
    private final LoginFailureHandler loginFailureHandler; // 로그인 실패 핸들러

    /**
     * SecurityFilterChain을 설정합니다.
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception Security 설정 실패 시 발생
     */
    @Bean
    public SecurityFilterChain config(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());

        return http.build();
    }

    /**
     * 로그인 인증 Provider 기반 AuthenticationManager를 생성합니다.
     *
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager providerConfig() {
        return new ProviderManager(List.of(loginAuthProvider));
    }

    /**
     * 로그인 처리 필터를 생성합니다.
     *
     * @return LoginProcessFilter
     */
    @Bean
    public LoginProcessFilter loginFilterConfig() {
        return new LoginProcessFilter(
                "/login", providerConfig(), loginSuccessHandler, loginFailureHandler, objectMapper);
    }
}
