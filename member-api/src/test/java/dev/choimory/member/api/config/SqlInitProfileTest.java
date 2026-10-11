package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.api.TestcontainersConfiguration;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

/**
 * 프로필별 SQL 초기화(sql init) 설정 테스트입니다.
 * SQL 초기화는 local 프로필에서만 동작해야 하며, dev와 prod 프로필에서는 동작하면 안 됩니다.
 */
class SqlInitProfileTest {

    /**
     * local 프로필의 SQL 초기화 설정 테스트입니다.
     * DDL이 작성되기 전에는 컨텍스트가 뜨지 않으므로 설정 파일을 직접 읽어 검증합니다.
     */
    @Nested
    class LocalProfile {

        /**
         * application-local.yml의 설정 값을 읽어 옵니다.
         *
         * @param key 조회할 설정 키
         * @return 설정 값. 없으면 null
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        private Object localProperty(String key) throws IOException {
            // application-local.yml을 읽어 프로퍼티 소스로 변환한다.
            List<PropertySource<?>> sources =
                    new YamlPropertySourceLoader().load("local", new ClassPathResource("application-local.yml"));

            // 첫 번째 프로퍼티 소스에서 설정 값을 조회한다.
            return sources.get(0).getProperty(key);
        }

        /**
         * local 프로필에서 SQL 초기화 모드가 always인지 확인합니다.
         *
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        @Test
        void sqlInitModeIsAlways() throws IOException {
            // Postgres에서도 스크립트가 실행되도록 always여야 한다.
            assertThat(localProperty("spring.sql.init.mode")).isEqualTo("always");
        }

        /**
         * local 프로필의 SQL 초기화 경로가 docs/db에서 복사된 DDL 파일을 가리키는지 확인합니다.
         *
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        @Test
        void sqlInitSchemaLocationPointsToDocsDdl() throws IOException {
            // docs/db에서 복사된 DDL 파일을 가리켜야 한다.
            assertThat(localProperty("spring.sql.init.schema-locations"))
                    .isEqualTo("classpath:/sql/v1/choimory-dev-member-ddl-v1.sql");
        }

        /**
         * 내용이 없는 DML은 Spring이 오류로 처리하므로 data-locations가 설정되지 않았는지 확인합니다.
         *
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        @Test
        void sqlInitDataLocationIsNotConfiguredWhileDmlIsEmpty() throws IOException {
            // 초기 데이터가 생기기 전까지는 data-locations를 설정하지 않는다.
            assertThat(localProperty("spring.sql.init.data-locations")).isNull();
        }

        /**
         * docs/db의 DDL, DML 파일이 클래스패스 sql/v1 아래로 복사되는지 확인합니다.
         */
        @Test
        void docsSqlFilesAreOnClasspath() {
            // 클래스패스에서 DDL, DML 파일을 조회한다.
            ClassPathResource ddl = new ClassPathResource("sql/v1/choimory-dev-member-ddl-v1.sql");
            ClassPathResource dml = new ClassPathResource("sql/v1/choimory-dev-member-dml-v1.sql");

            // 두 파일 모두 존재해야 한다.
            assertThat(ddl.exists()).isTrue();
            assertThat(dml.exists()).isTrue();
        }
    }

    /**
     * dev 프로필의 SQL 초기화 설정 테스트입니다.
     */
    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest
    @ActiveProfiles("dev")
    class DevProfile {

        @Autowired
        private Environment environment; // 스프링 환경 설정

        /**
         * dev 프로필에서는 SQL 초기화가 설정되지 않는지 확인합니다.
         */
        @Test
        void sqlInitIsNotConfigured() {
            // dev 프로필의 SQL 초기화 설정을 조회한다.
            String mode = environment.getProperty("spring.sql.init.mode");

            // always가 아니어야 한다.
            assertThat(mode).isNotEqualTo("always");
        }
    }

    /**
     * prod 프로필의 SQL 초기화 설정 테스트입니다.
     */
    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest
    @ActiveProfiles("prod")
    class ProdProfile {

        @Autowired
        private Environment environment; // 스프링 환경 설정

        /**
         * prod 프로필에서는 SQL 초기화가 설정되지 않는지 확인합니다.
         */
        @Test
        void sqlInitIsNotConfigured() {
            // prod 프로필의 SQL 초기화 설정을 조회한다.
            String mode = environment.getProperty("spring.sql.init.mode");

            // always가 아니어야 한다.
            assertThat(mode).isNotEqualTo("always");
        }
    }
}
