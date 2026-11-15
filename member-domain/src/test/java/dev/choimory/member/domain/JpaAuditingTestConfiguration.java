package dev.choimory.member.domain;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing을 켜는 테스트 설정입니다. 수정 이력이 없으면 modified_at이 null이도록 저장 시에는 수정 일시를 채우지 않습니다.
 * (애플리케이션 설정과 같아야 합니다)
 * 엔티티가 없는 슬라이스 테스트(@JdbcTest 등)에서는 JPA 메타모델이 비어 있어 Auditing을 켤 수 없으므로,
 * 엔티티를 사용하는 JPA 테스트에서만 가져와(@Import) 사용합니다.
 */
@TestConfiguration
@EnableJpaAuditing(modifyOnCreate = false)
public class JpaAuditingTestConfiguration {}
