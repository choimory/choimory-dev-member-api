package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

/**
 * DTO 작성 규칙 테스트입니다. 새 DTO에서 규칙을 빠뜨리면 이 테스트가 실패합니다.
 * <ul>
 *   <li>DTO는 불변이므로 record로 작성하고 이름은 엔티티명 + Dto입니다.</li>
 *   <li>Entity와 DTO는 1:1로 매칭합니다.</li>
 *   <li>변환은 DTO의 정적 팩토리 from(Entity)가 담당합니다. (Entity에는 DTO 변환 메소드를 두지 않습니다)</li>
 *   <li>Builder 패턴(@Builder(toBuilder = true))으로 작성합니다.</li>
 * </ul>
 */
class DtoRulesTest {

    /**
     * 모든 DTO가 record이고 이름이 Dto로 끝나는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void everyDtoIsRecordNamedWithDtoSuffix() throws ClassNotFoundException {
        // DTO 클래스를 조회한다.
        List<Class<?>> dtos = findDtos();

        // 한 개 이상 있어야 하고 모두 record이며 이름이 Dto로 끝나야 한다.
        assertThat(dtos).isNotEmpty();
        assertThat(dtos).allMatch(Class::isRecord);
        assertThat(dtos).allMatch(type -> type.getSimpleName().endsWith("Dto"));
    }

    /**
     * 엔티티와 DTO가 1:1로 매칭되는지(엔티티마다 {엔티티명}Dto가 있고 그 외의 DTO가 없는지) 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void entitiesAndDtosMatchOneToOne() throws ClassNotFoundException {
        // 엔티티 이름에 Dto를 붙인 집합과 DTO 이름 집합을 만든다.
        Set<String> expected = new TreeSet<>();
        for (Class<?> entity : findEntities()) {
            expected.add(entity.getSimpleName() + "Dto");
        }
        Set<String> actual = new TreeSet<>();
        for (Class<?> dto : findDtos()) {
            actual.add(dto.getSimpleName());
        }

        // 두 집합이 같아야 한다.
        assertThat(actual).isEqualTo(expected);
    }

    /**
     * 모든 DTO가 정적 팩토리 from(Entity)와 toBuilder()를 가지고 Builder 클래스가 있는지 확인합니다.
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void everyDtoHasFromToBuilderAndBuilder() throws ClassNotFoundException {
        for (Class<?> dto : findDtos()) {
            // from(Entity): 엔티티 하나를 받아 DTO를 반환하는 정적 메소드
            String entityName =
                    dto.getSimpleName().substring(0, dto.getSimpleName().length() - "Dto".length());
            Method from = Arrays.stream(dto.getDeclaredMethods())
                    .filter(method -> method.getName().equals("from"))
                    .findFirst()
                    .orElse(null);
            assertThat(from).as(dto.getSimpleName() + ".from").isNotNull();
            assertThat(Modifier.isStatic(from.getModifiers())).isTrue();
            assertThat(from.getParameterCount()).isEqualTo(1);
            assertThat(from.getParameterTypes()[0].getSimpleName())
                    .as(dto.getSimpleName())
                    .isEqualTo(entityName);
            assertThat(from.getReturnType()).isEqualTo(dto);

            // toBuilder(): @Builder(toBuilder = true)
            assertThat(Arrays.stream(dto.getDeclaredMethods())
                            .anyMatch(method -> method.getName().equals("toBuilder")))
                    .as(dto.getSimpleName() + ".toBuilder")
                    .isTrue();

            // {DTO명}Builder 클래스: @Builder
            assertThat(Arrays.stream(dto.getDeclaredClasses())
                            .anyMatch(inner -> inner.getSimpleName().equals(dto.getSimpleName() + "Builder")))
                    .as(dto.getSimpleName() + " Builder")
                    .isTrue();
        }
    }

    /**
     * 엔티티에는 DTO 변환 메소드가 없는지 확인합니다. (변환 책임은 DTO에 있다)
     *
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    @Test
    void entitiesDoNotHaveDtoConversionMethods() throws ClassNotFoundException {
        for (Class<?> entity : findEntities()) {
            boolean hasConversion = Arrays.stream(entity.getDeclaredMethods())
                    .anyMatch(method -> method.getName().startsWith("toDto")
                            || method.getReturnType().getSimpleName().endsWith("Dto"));
            assertThat(hasConversion).as(entity.getSimpleName()).isFalse();
        }
    }

    /**
     * dev.choimory.member.domain의 command.dto 패키지에서 이름이 Dto로 끝나는 클래스(DTO)를 찾습니다.
     *
     * @return DTO 클래스 목록
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    private List<Class<?>> findDtos() throws ClassNotFoundException {
        // command.dto 패키지의 클래스를 스캔한다.
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*\\.command\\.dto\\.\\w+Dto")));

        // 같은 패키지의 테스트 클래스(...DtosTest)와 중첩 클래스(Builder 등)는 필터의 이름 규칙(...Dto)으로 제외된다.
        List<Class<?>> dtos = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("dev.choimory.member.domain")) {
            dtos.add(Class.forName(definition.getBeanClassName()));
        }
        return dtos;
    }

    /**
     * dev.choimory.member.domain에서 BaseEntity를 상속한 구체 엔티티 클래스를 찾습니다.
     *
     * @return 엔티티 클래스 목록
     * @throws ClassNotFoundException 클래스를 로드하지 못한 경우 발생
     */
    private List<Class<?>> findEntities() throws ClassNotFoundException {
        // BaseEntity를 상속한 클래스를 스캔한다.
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEntity.class));

        // 구체 클래스만 모은다.
        List<Class<?>> entities = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("dev.choimory.member.domain")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (!Modifier.isAbstract(type.getModifiers())) {
                entities.add(type);
            }
        }
        return entities;
    }
}
