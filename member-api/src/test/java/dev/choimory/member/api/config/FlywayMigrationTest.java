package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.api.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 앱이 뜰 때 Flyway가 docs/flyway의 마이그레이션을 실제로 적용하는지 확인하는 테스트입니다.
 * ddl-auto=validate로 실행하므로 엔티티 매핑과 마이그레이션으로 만들어진 스키마가 일치하는지도 함께 검증합니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@ActiveProfiles("local")
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    /**
     * Flyway 이력에 V1이 성공으로 기록되었는지 확인합니다.
     */
    @Test
    void initSchemaIsRecordedAsSuccess() {
        // 성공한 마이그레이션 이력을 버전 순으로 조회한다.
        List<String> versions = jdbc.queryForList(
                "select version from flyway_schema_history where success = true and version is not null"
                        + " order by installed_rank",
                String.class);

        // V1이 첫 번째로 적용되어 있어야 한다.
        assertThat(versions).startsWith("1");
    }

    /**
     * 마이그레이션으로 DDL의 테이블이 모두 생성되었는지 확인합니다.
     */
    @Test
    void migrationCreatesAllTables() {
        // 현재 스키마의 테이블 이름을 조회한다.
        List<String> names = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()", String.class);

        // DDL의 테이블이 생성되어 있어야 한다.
        assertThat(names)
                .contains(
                        "member",
                        "member_image",
                        "member_agreement",
                        "follow",
                        "member_suspension",
                        "member_authority",
                        "authority",
                        "permission",
                        "permission_action",
                        "menu",
                        "agreement",
                        "agreement_answer");
    }
}
