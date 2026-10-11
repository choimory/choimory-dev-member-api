package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import dev.choimory.member.api.TestcontainersConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;

/**
 * local 프로필에서 앱이 뜰 때 SQL 초기화가 docs/db의 DDL을 실제로 실행하는지 확인하는 테스트입니다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("local")
class LocalProfileSqlInitContextTest {

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    @Autowired
    private DataSource dataSource; // 데이터 소스

    /**
     * 앱이 뜨는 시점에 SQL 초기화로 DDL의 테이블이 생성되었는지 확인합니다.
     */
    @Test
    void ddlIsAppliedOnStartup() {
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

    /**
     * 이미 DDL이 적용된 상태에서 앱이 다시 떠도 오류가 없도록 DDL을 다시 실행해도 안전한지 확인합니다.
     */
    @Test
    void ddlCanBeAppliedAgain() {
        // 클래스패스로 복사된 DDL을 다시 실행한다.
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("sql/v1/choimory-dev-member-ddl-v1.sql"));

        // 오류 없이 실행되어야 한다.
        assertThatCode(() -> populator.execute(dataSource)).doesNotThrowAnyException();
    }
}
