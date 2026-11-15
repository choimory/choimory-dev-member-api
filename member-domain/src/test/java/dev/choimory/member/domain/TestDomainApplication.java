package dev.choimory.member.domain;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * member-domain 모듈의 JPA 테스트용 설정 클래스입니다.
 * 이 모듈에는 애플리케이션 클래스가 없으므로 슬라이스 테스트(@DataJpaTest)가 찾을 수 있도록 테스트 소스에 둡니다.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class TestDomainApplication {}
