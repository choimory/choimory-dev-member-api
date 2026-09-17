package dev.choimory.member.api;

import org.springframework.boot.SpringApplication;

/**
 * Testcontainers 설정을 포함해 애플리케이션을 실행하는 테스트용 진입점입니다.
 */
public class TestChoimoryDevMemberApiApplication {

    /**
     * 테스트용 애플리케이션을 실행합니다.
     *
     * @param args 실행 인자
     */
    public static void main(String[] args) {
        SpringApplication.from(ChoimoryDevMemberApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
