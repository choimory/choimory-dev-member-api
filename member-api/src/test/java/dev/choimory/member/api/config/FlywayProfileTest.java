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
 * 프로필별 Flyway 설정 테스트입니다.
 * local, dev, prod 프로필 모두 flyway 프로필이 함께 활성화되어 마이그레이션이 적용되어야 합니다.
 */
class FlywayProfileTest {

    /**
     * 프로필 설정 파일의 설정 값을 읽어 옵니다.
     *
     * @param file 클래스패스의 yml 파일명
     * @param key 조회할 설정 키
     * @return 설정 값. 없으면 null
     * @throws IOException 설정 파일을 읽지 못한 경우 발생
     */
    private static Object yamlProperty(String file, String key) throws IOException {
        // yml을 읽어 프로퍼티 소스로 변환한 뒤 첫 번째 소스에서 값을 조회한다.
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
        return sources.get(0).getProperty(key);
    }

    /**
     * 설정 파일과 클래스패스 리소스 테스트입니다.
     */
    @Nested
    class Files {

        /**
         * 마이그레이션 SQL이 클래스패스 db/migration 아래로 복사되는지 확인합니다.
         */
        @Test
        void migrationScriptIsOnClasspath() {
            // 클래스패스에서 V1 마이그레이션을 조회한다.
            ClassPathResource script = new ClassPathResource("db/migration/v1/V1__init_schema.sql");

            // 존재해야 한다.
            assertThat(script.exists()).isTrue();
        }

        /**
         * application-flyway.yml이 flyway 프로필에서만 활성화되고 clean이 막혀 있는지 확인합니다.
         *
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        @Test
        void flywayYmlIsActivatedByFlywayProfileAndCleanIsDisabled() throws IOException {
            // flyway 프로필에서만 활성화되어야 한다.
            assertThat(yamlProperty("application-flyway.yml", "spring.config.activate.on-profile"))
                    .isEqualTo("flyway");

            // 마이그레이션 위치가 지정되고 clean은 막혀 있어야 한다.
            assertThat(yamlProperty("application-flyway.yml", "spring.flyway.locations"))
                    .isEqualTo("classpath:db/migration");
            assertThat(yamlProperty("application-flyway.yml", "spring.flyway.clean-disabled"))
                    .isEqualTo(true);
        }

        /**
         * application-prod.yml이 prod 프로필에서 활성화되고 SQL 로그를 출력하지 않는지 확인합니다.
         *
         * @throws IOException 설정 파일을 읽지 못한 경우 발생
         */
        @Test
        void prodYmlIsActivatedByProdProfileAndDoesNotLogSql() throws IOException {
            // prod 프로필에서 활성화되어야 한다.
            assertThat(yamlProperty("application-prod.yml", "spring.config.activate.on-profile"))
                    .isEqualTo("prod");

            // SQL 로그를 출력하지 않아야 한다.
            assertThat(yamlProperty("application-prod.yml", "spring.jpa.show-sql"))
                    .isEqualTo(false);
            assertThat(yamlProperty("application-prod.yml", "spring.jpa.properties.hibernate.show_sql"))
                    .isEqualTo(false);
            assertThat(yamlProperty("application-prod.yml", "spring.jpa.properties.hibernate.format_sql"))
                    .isEqualTo(false);
        }

        /**
         * sql-init 설정 파일이 제거되었는지 확인합니다.
         */
        @Test
        void sqlInitYmlIsRemoved() {
            // Flyway로 대체되어 파일이 없어야 한다.
            assertThat(new ClassPathResource("application-sql-init.yml").exists())
                    .isFalse();
        }
    }

    /**
     * local 프로필 테스트입니다.
     */
    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest
    @ActiveProfiles("local")
    class LocalProfile {

        @Autowired
        private Environment environment; // 스프링 환경 설정

        /**
         * local 프로필에서 flyway 프로필이 함께 활성화되는지 확인합니다.
         */
        @Test
        void flywayIsEnabled() {
            assertThat(environment.getActiveProfiles()).contains("local", "flyway");
            assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
            assertThat(environment.getProperty("spring.sql.init.mode")).isNotEqualTo("always");
        }
    }

    /**
     * dev 프로필 테스트입니다.
     */
    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest
    @ActiveProfiles("dev")
    class DevProfile {

        @Autowired
        private Environment environment; // 스프링 환경 설정

        /**
         * dev 프로필에서 flyway 프로필이 함께 활성화되는지 확인합니다.
         */
        @Test
        void flywayIsEnabled() {
            assertThat(environment.getActiveProfiles()).contains("dev", "flyway");
            assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
        }
    }

    /**
     * prod 프로필 테스트입니다.
     */
    @Nested
    @Import(TestcontainersConfiguration.class)
    @SpringBootTest
    @ActiveProfiles("prod")
    class ProdProfile {

        @Autowired
        private Environment environment; // 스프링 환경 설정

        /**
         * prod 프로필에서 flyway 프로필이 함께 활성화되고 SQL 로그가 꺼져 있는지 확인합니다.
         */
        @Test
        void flywayIsEnabledAndSqlLogIsOff() {
            assertThat(environment.getActiveProfiles()).contains("prod", "flyway");
            assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
            assertThat(environment.getProperty("spring.jpa.show-sql")).isEqualTo("false");
            assertThat(environment.getProperty("spring.jpa.properties.hibernate.format_sql"))
                    .isEqualTo("false");
        }
    }
}
