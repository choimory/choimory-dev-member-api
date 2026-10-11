package dev.choimory.member.domain;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * member-domain 모듈 테스트용 Testcontainers 설정입니다.
 * docs/db의 DDL을 컨테이너 초기화 스크립트로 적용하므로 테스트 스키마가 실제 DDL과 같습니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * PostgreSQL 16 테스트 컨테이너 Bean을 생성합니다.
     * 시스템 프로퍼티 docs.db.ddl이 가리키는 DDL 파일을 초기화 스크립트로 적용합니다.
     *
     * @return PostgreSQL 테스트 컨테이너
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        // docs/db의 DDL 파일 경로를 조회한다.
        String ddlPath = System.getProperty("docs.db.ddl");

        // DDL 파일을 컨테이너 초기화 스크립트로 복사한다.
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withCopyFileToContainer(MountableFile.forHostPath(ddlPath), "/docker-entrypoint-initdb.d/01-ddl.sql");
    }
}
