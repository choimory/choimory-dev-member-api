package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V2(timestamp -> timestamptz) 마이그레이션이 기존 데이터를 올바른 시각으로 변환하는지 확인하는 테스트입니다.
 * V1까지만 적용해 timestamp 값(Asia/Seoul 기준으로 쓴 값)을 넣은 뒤 V2를 적용해 UTC 시각으로 바뀌는지 검증합니다.
 */
@Testcontainers
class FlywayTimestamptzMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16"); // 테스트용 DB

    /**
     * 마이그레이션 대상 버전까지만 적용하는 Flyway를 생성합니다.
     *
     * @param target 적용할 마지막 버전
     * @return Flyway
     */
    private Flyway flyway(String target) {
        // classpath db/migration의 SQL을 지정한 버전까지만 적용하도록 설정한다.
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    /**
     * V1로 저장된 timestamp 값이 V2 적용 후 Asia/Seoul 기준의 올바른 UTC 시각으로 변환되는지 확인합니다.
     *
     * @throws SQLException JDBC 처리 중 발생
     */
    @Test
    void v2ConvertsSeoulTimestampToUtcInstant() throws SQLException {
        // V1까지만 적용하고 시간대 없는 timestamp 값(서울 09:00)을 가진 회원을 저장한다.
        flyway("1").migrate();
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute("insert into member (id, email, password, nickname, status, created_at)"
                    + " values ('m1', 'a@example.com', 'pw', 'nick', 'ACTIVATE', timestamp '2026-10-14 09:00:00')");

            // V1 상태에서는 시간대 없는 timestamp여야 한다.
            assertThat(dataType(statement)).isEqualTo("timestamp without time zone");
        }

        // V2까지 적용한다.
        flyway("2").migrate();

        // 컬럼이 timestamptz로 바뀌고 값이 UTC 00:00(서울 09:00)으로 변환되어야 한다.
        try (Connection connection = DriverManager.getConnection(
                        POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            assertThat(dataType(statement)).isEqualTo("timestamp with time zone");

            try (ResultSet resultSet = statement.executeQuery("select created_at from member where id = 'm1'")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet
                                .getObject(1, java.time.OffsetDateTime.class)
                                .toInstant())
                        .isEqualTo(Instant.parse("2026-10-14T00:00:00Z"));
            }
        }
    }

    /**
     * member.created_at 컬럼의 데이터 타입을 조회합니다.
     *
     * @param statement JDBC 문장
     * @return 컬럼의 데이터 타입
     * @throws SQLException JDBC 처리 중 발생
     */
    private String dataType(Statement statement) throws SQLException {
        // information_schema에서 컬럼 타입을 조회한다.
        try (ResultSet resultSet = statement.executeQuery("select data_type from information_schema.columns"
                + " where table_schema = current_schema() and table_name = 'member' and column_name = 'created_at'")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }
}
