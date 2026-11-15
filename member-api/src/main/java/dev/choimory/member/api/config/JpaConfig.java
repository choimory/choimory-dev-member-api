package dev.choimory.member.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 스캔 설정입니다.
 * 엔티티는 member-domain 모듈에, 레포지토리는 member-api 모듈에 있으므로 스캔 패키지를 명시합니다.
 */
@Configuration
@EntityScan(basePackages = "dev.choimory.member.domain")
@EnableJpaRepositories(basePackages = "dev.choimory.member.api")
public class JpaConfig {}
