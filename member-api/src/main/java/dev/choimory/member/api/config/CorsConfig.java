package dev.choimory.member.api.config;

import dev.choimory.member.api.config.properties.CorsProperties;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS 설정입니다.
 * 허용 Origin은 환경별 프로퍼티로 받고, 메서드와 헤더 등은 환경과 관계없이 같아 상수로 둡니다.
 * 쿠키를 사용하지 않는 토큰 기반 API이므로 자격 증명(allowCredentials)은 허용하지 않습니다.
 * Spring Security의 cors 설정이 이 빈(corsConfigurationSource)을 사용해 인증 필터보다 앞에서 preflight를 처리합니다.
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS = List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name()); // 허용 메서드
    private static final List<String> ALLOWED_HEADERS =
            List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE); // 허용 요청 헤더
    private static final List<String> EXPOSED_HEADERS = List.of(HttpHeaders.AUTHORIZATION); // 브라우저에 노출할 응답 헤더
    private static final Duration MAX_AGE = Duration.ofSeconds(3600); // preflight 결과 캐시 시간

    private final CorsProperties corsProperties; // 허용 Origin 설정

    /**
     * 모든 경로에 적용되는 CORS 설정 소스를 구성합니다.
     *
     * @return CORS 설정 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
