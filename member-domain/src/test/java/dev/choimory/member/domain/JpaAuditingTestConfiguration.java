package dev.choimory.member.domain;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing을 켜는 테스트 설정입니다.
 * 엔티티가 없는 슬라이스 테스트(@JdbcTest 등)에서는 JPA 메타모델이 비어 있어 Auditing을 켤 수 없으므로,
 * 엔티티를 사용하는 JPA 테스트에서만 가져와(@Import) 사용합니다.
 */
@TestConfiguration
@EnableJpaAuditing
public class JpaAuditingTestConfiguration {}
