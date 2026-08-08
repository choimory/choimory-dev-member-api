package dev.choimory.member.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Choimory 개발 회원 API 애플리케이션 실행 클래스입니다.
 */
@SpringBootApplication
@EnableJpaAuditing
public class ChoimoryDevMemberApiApplication {
    /**
     * Spring Boot 애플리케이션을 실행합니다.
     *
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.run(ChoimoryDevMemberApiApplication.class, args);
    }
}
