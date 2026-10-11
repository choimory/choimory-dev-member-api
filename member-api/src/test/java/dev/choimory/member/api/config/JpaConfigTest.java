package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 스캔 설정 테스트입니다.
 */
class JpaConfigTest {

    /**
     * 엔티티 스캔 대상이 member-domain 모듈의 패키지인지 확인합니다.
     */
    @Test
    void entityScanTargetsDomainModule() {
        // JpaConfig에 선언된 엔티티 스캔 설정을 조회한다.
        EntityScan entityScan = JpaConfig.class.getAnnotation(EntityScan.class);

        // member-domain 모듈의 기본 패키지를 스캔해야 한다.
        assertThat(entityScan).isNotNull();
        assertThat(entityScan.basePackages()).containsExactly("dev.choimory.member.domain");
    }

    /**
     * 레포지토리 스캔 대상이 member-api 모듈의 패키지인지 확인합니다.
     */
    @Test
    void jpaRepositoriesScanTargetsApiModule() {
        // JpaConfig에 선언된 레포지토리 스캔 설정을 조회한다.
        EnableJpaRepositories repositories = JpaConfig.class.getAnnotation(EnableJpaRepositories.class);

        // 레포지토리는 member-api 모듈에 있으므로 api 기본 패키지를 스캔해야 한다.
        assertThat(repositories).isNotNull();
        assertThat(repositories.basePackages()).containsExactly("dev.choimory.member.api");
    }
}
