package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import dev.choimory.member.testsupport.NoRestrictionEntity;
import dev.choimory.member.testsupport.TableAnnotatedEntity;
import dev.choimory.member.testsupport.TestBaseEntity;
import jakarta.persistence.Table;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

/**
 * 엔티티 작성 규칙 테스트입니다. 새 엔티티에서 규칙을 빠뜨리면 이 테스트가 실패합니다.
 * <ul>
 *   <li>BaseEntity에 붙인 @SQLRestriction은 하위 엔티티에 상속되지 않으므로 모든 엔티티가 직접 선언해야 합니다.</li>
 *   <li>엔티티는 정적 팩토리(create) 없이 클래스 레벨 Lombok Builder로 생성합니다.
 *       (코드 컨벤션: 생성자를 직접 쓰지 않고 Builder 패턴을 이용한다)</li>
 *   <li>테이블 이름은 엔티티 이름을 따르므로 @Table을 붙이지 않습니다.</li>
 * </ul>
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
        List<String> violations = names(withoutSoftDeleteRestriction(findEntities("dev.choimory.member.domain")));

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
        List<String> violations = names(withoutSoftDeleteRestriction(findEntities("dev.choimory.member.testsupport")));

        // 필터가 없는 클래스만 걸리고, 필터가 있는 TestBaseEntity와 TableAnnotatedEntity는 걸리지 않아야 한다.
        assertThat(violations).containsExactly(NoRestrictionEntity.class.getName());
    }

    /**
     * 실제 엔티티(dev.choimory.member.domain)가 모두 Lombok Builder로 생성되도록 선언되었는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void everyEntityIsBuiltWithBuilder() throws ClassNotFoundException {
        // 실제 엔티티 패키지에서 Builder가 없는 엔티티를 조회한다.
        List<String> violations = names(withoutBuilder(findEntities("dev.choimory.member.domain")));

        // 모든 엔티티가 Builder를 가져야 한다.
        assertThat(violations).isEmpty();
    }

    /**
     * 검사 로직이 Builder가 없는 엔티티를 실제로 찾아내는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void checkDetectsEntityWithoutBuilder() throws ClassNotFoundException {
        // 테스트 전용 패키지에서 Builder가 없는 엔티티를 조회한다.
        List<String> violations = names(withoutBuilder(findEntities("dev.choimory.member.testsupport")));

        // 테스트 전용 엔티티는 Builder가 없으므로 모두 걸려야 한다.
        assertThat(violations)
                .containsExactlyInAnyOrder(
                        NoRestrictionEntity.class.getName(),
                        TableAnnotatedEntity.class.getName(),
                        TestBaseEntity.class.getName());
    }

    /**
     * 실제 엔티티(dev.choimory.member.domain)에 정적 팩토리 create가 없는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void noEntityDeclaresCreateFactory() throws ClassNotFoundException {
        // 실제 엔티티 패키지에서 정적 create 메소드가 있는 엔티티를 조회한다.
        List<String> violations = names(withCreateFactory(findEntities("dev.choimory.member.domain")));

        // 정적 create 메소드가 없어야 한다.
        assertThat(violations).isEmpty();
    }

    /**
     * 검사 로직이 정적 create 메소드가 있는 클래스를 실제로 찾아내는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void checkDetectsEntityWithCreateFactory() throws ClassNotFoundException {
        // 테스트 전용 패키지에서 정적 create 메소드가 있는 클래스를 조회한다.
        List<String> violations = names(withCreateFactory(findEntities("dev.choimory.member.testsupport")));

        // TestBaseEntity만 create를 가진다.
        assertThat(violations).containsExactly(TestBaseEntity.class.getName());
    }

    /**
     * 실제 엔티티(dev.choimory.member.domain)에 @Table이 없는지 확인합니다. (테이블 이름은 엔티티 이름을 따른다)
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void noEntityDeclaresTableAnnotation() throws ClassNotFoundException {
        // 실제 엔티티 패키지에서 @Table이 붙은 엔티티를 조회한다.
        List<String> violations = names(withTableAnnotation(findEntities("dev.choimory.member.domain")));

        // @Table이 붙은 엔티티가 없어야 한다.
        assertThat(violations).isEmpty();
    }

    /**
     * 검사 로직이 @Table이 붙은 클래스를 실제로 찾아내는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void checkDetectsEntityWithTableAnnotation() throws ClassNotFoundException {
        // 테스트 전용 패키지에서 @Table이 붙은 클래스를 조회한다.
        List<String> violations = names(withTableAnnotation(findEntities("dev.choimory.member.testsupport")));

        // @Table이 붙은 클래스만 걸려야 한다.
        assertThat(violations).containsExactly(TableAnnotatedEntity.class.getName());
    }

    /**
     * 패키지에서 BaseEntity를 상속한 구체 클래스를 찾습니다.
     *
     * @param basePackage 검사할 기본 패키지
     * @return 구체 엔티티 클래스 목록
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    private List<Class<?>> findEntities(String basePackage) throws ClassNotFoundException {
        // BaseEntity를 상속한 클래스를 스캔한다. (추상 클래스도 후보에 포함하고 직접 거른다)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEntity.class));

        // 구체 클래스만 모은다.
        List<Class<?>> entities = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (!Modifier.isAbstract(type.getModifiers())) {
                entities.add(type);
            }
        }
        return entities;
    }

    /**
     * @SQLRestriction("deleted_at IS NULL")이 없는 클래스만 걸러냅니다.
     *
     * @param entities 검사할 엔티티 클래스 목록
     * @return 규칙을 위반한 클래스 목록
     */
    private List<Class<?>> withoutSoftDeleteRestriction(List<Class<?>> entities) {
        return entities.stream()
                .filter(type -> {
                    SQLRestriction restriction = type.getAnnotation(SQLRestriction.class);
                    return restriction == null || !SOFT_DELETE_CONDITION.equals(restriction.value());
                })
                .toList();
    }

    /**
     * Lombok Builder가 만든 중첩 클래스({클래스명}Builder)가 없는 클래스만 걸러냅니다.
     *
     * @param entities 검사할 엔티티 클래스 목록
     * @return 규칙을 위반한 클래스 목록
     */
    private List<Class<?>> withoutBuilder(List<Class<?>> entities) {
        return entities.stream()
                .filter(type -> Arrays.stream(type.getDeclaredClasses())
                        .noneMatch(inner -> inner.getSimpleName().equals(type.getSimpleName() + "Builder")))
                .toList();
    }

    /**
     * 정적 create 메소드가 있는 클래스만 걸러냅니다.
     *
     * @param entities 검사할 엔티티 클래스 목록
     * @return 규칙을 위반한 클래스 목록
     */
    private List<Class<?>> withCreateFactory(List<Class<?>> entities) {
        return entities.stream()
                .filter(type -> Arrays.stream(type.getDeclaredMethods())
                        .anyMatch(method ->
                                method.getName().equals("create") && Modifier.isStatic(method.getModifiers())))
                .toList();
    }

    /**
     * @Table이 붙은 클래스만 걸러냅니다.
     *
     * @param entities 검사할 엔티티 클래스 목록
     * @return 규칙을 위반한 클래스 목록
     */
    private List<Class<?>> withTableAnnotation(List<Class<?>> entities) {
        return entities.stream()
                .filter(type -> type.getAnnotation(Table.class) != null)
                .toList();
    }

    /**
     * 클래스 목록을 이름 목록으로 변환합니다.
     *
     * @param types 클래스 목록
     * @return 클래스 이름 목록
     */
    private List<String> names(List<Class<?>> types) {
        return types.stream().map(Class::getName).toList();
    }
}
