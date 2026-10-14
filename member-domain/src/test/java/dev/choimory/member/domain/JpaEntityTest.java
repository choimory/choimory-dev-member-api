package dev.choimory.member.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 엔티티 테스트용 메타 어노테이션입니다.
 * docs/flyway의 마이그레이션 SQL이 적용된 Postgres(Testcontainers)에서 ddl-auto=validate로 실행하므로
 * 엔티티 매핑이 DDL과 어긋나면 컨텍스트가 뜨지 않습니다. 테스트마다 트랜잭션이 롤백됩니다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaAuditingTestConfiguration.class})
public @interface JpaEntityTest {}
