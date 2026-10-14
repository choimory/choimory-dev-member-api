package dev.choimory.member.domain;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * member-domain 모듈 테스트용 Testcontainers 설정입니다.
 * docs/flyway의 마이그레이션 SQL을 버전 순으로 컨테이너 초기화 스크립트로 적용하므로 테스트 스키마가 실제 마이그레이션과 같습니다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * PostgreSQL 16 테스트 컨테이너 Bean을 생성합니다.
     * 시스템 프로퍼티 docs.flyway.dir 아래의 마이그레이션 SQL을 버전 순으로 초기화 스크립트로 적용합니다.
     *
     * @return PostgreSQL 테스트 컨테이너
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        // 컨테이너를 생성한다.
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));

        // 마이그레이션 SQL을 버전 순서가 파일명 정렬과 같도록 번호를 붙여 초기화 스크립트로 복사한다.
        List<Path> files = MigrationScripts.files();
        for (int i = 0; i < files.size(); i++) {
            container.withCopyFileToContainer(
                    MountableFile.forHostPath(files.get(i)),
                    String.format(
                            "/docker-entrypoint-initdb.d/%03d-%s",
                            i + 1, files.get(i).getFileName()));
        }
        return container;
    }
}
