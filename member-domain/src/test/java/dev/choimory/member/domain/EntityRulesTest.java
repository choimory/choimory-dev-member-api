package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import dev.choimory.member.testsupport.NoRestrictionEntity;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * 엔티티 작성 규칙 테스트입니다.
 * BaseEntity에 붙인 @SQLRestriction은 하위 엔티티에 상속되지 않으므로 모든 엔티티가 직접 선언해야 합니다.
 * 새 엔티티에서 선언을 빠뜨리면 이 테스트가 실패합니다.
 */
class EntityRulesTest {

    private static final String SOFT_DELETE_CONDITION = "deleted_at IS NULL"; // soft delete 필터 조건

    /**
     * 실제 엔티티(dev.choimory.member.domain) 중 soft delete 필터가 없는 엔티티가 없는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void everyEntityDeclaresSoftDeleteRestriction() throws ClassNotFoundException {
        // 실제 엔티티 패키지에서 필터가 없는 엔티티를 조회한다.
        List<String> violations = findEntitiesWithoutSoftDeleteRestriction("dev.choimory.member.domain");

        // 모든 엔티티가 필터를 선언해야 한다.
        assertThat(violations).isEmpty();
    }

    /**
     * 검사 로직이 필터가 없는 엔티티를 실제로 찾아내고, 필터가 있는 엔티티는 통과시키는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void checkDetectsEntityWithoutRestriction() throws ClassNotFoundException {
        // 테스트 전용 패키지에서 필터가 없는 엔티티를 조회한다.
        List<String> violations = findEntitiesWithoutSoftDeleteRestriction("dev.choimory.member.testsupport");

        // 필터가 없는 클래스만 걸리고, 필터가 있는 TestBaseEntity는 걸리지 않아야 한다.
        assertThat(violations).containsExactly(NoRestrictionEntity.class.getName());
    }

    /**
     * 패키지에서 BaseEntity를 상속한 구체 클래스 중 @SQLRestriction("deleted_at IS NULL")이 없는 클래스를 찾습니다.
     *
     * @param basePackage 검사할 기본 패키지
     * @return 규칙을 위반한 클래스 이름 목록
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    private List<String> findEntitiesWithoutSoftDeleteRestriction(String basePackage) throws ClassNotFoundException {
        // BaseEntity를 상속한 클래스를 스캔한다. (추상 클래스도 후보에 포함하고 직접 거른다)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEntity.class));

        // 구체 클래스 중 필터를 선언하지 않은 것을 모은다.
        List<String> violations = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (Modifier.isAbstract(type.getModifiers())) {
                continue;
            }
            SQLRestriction restriction = type.getAnnotation(SQLRestriction.class);
            if (restriction == null || !SOFT_DELETE_CONDITION.equals(restriction.value())) {
                violations.add(type.getName());
            }
        }
        return violations;
    }
}
