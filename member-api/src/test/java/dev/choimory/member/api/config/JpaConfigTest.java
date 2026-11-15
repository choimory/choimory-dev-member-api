package dev.choimory.member.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.api.ChoimoryDevMemberApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
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

    /**
     * JPA Auditing이 켜져 있고 수정 이력이 없으면 modified_at이 null이도록(저장 시 채우지 않도록) 설정되었는지 확인합니다.
     */
    @Test
    void auditingDoesNotModifyOnCreation() {
        // 애플리케이션 클래스에 선언된 Auditing 설정을 조회한다.
        EnableJpaAuditing auditing = ChoimoryDevMemberApiApplication.class.getAnnotation(EnableJpaAuditing.class);

        // 저장 시에는 createdAt만 채우고 modifiedAt은 수정할 때 채워야 한다.
        assertThat(auditing).isNotNull();
        assertThat(auditing.modifyOnCreate()).isFalse();
    }
}
