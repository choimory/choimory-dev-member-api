package dev.choimory.member.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 설정입니다.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * BCrypt 비밀번호 인코더 Bean을 생성합니다.
     *
     * @return 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
