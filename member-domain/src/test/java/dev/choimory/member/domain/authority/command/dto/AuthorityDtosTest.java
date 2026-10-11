package dev.choimory.member.domain.authority.command.dto;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import dev.choimory.member.domain.authority.command.entity.Menu;
import dev.choimory.member.domain.authority.command.entity.Permission;
import dev.choimory.member.domain.authority.command.entity.PermissionAction;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.authority.command.type.PermissionActionType;
import dev.choimory.member.domain.member.command.dto.MemberDto;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * authority 패키지 DTO(AuthorityDto, MenuDto, PermissionDto, PermissionActionDto, MemberAuthorityDto) 테스트입니다.
 */
@JpaEntityTest
class AuthorityDtosTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    /**
     * 권한 DTO가 소유한 permission과 action을 하위 DTO로 담고 메뉴는 하위 컬렉션 없이 담는지 확인합니다.
     */
    @Test
    void authorityDtoContainsPermissionsAndActions() {
        // 권한, 하위 메뉴가 있는 메뉴, permission, action 2개를 만든다. (DB를 사용하지 않는다)
        Authority authority =
                Authority.builder().name("관리자").code(AuthorityCode.ADMIN).build();
        Menu menu = Menu.builder()
                .parent(null)
                .name("메뉴")
                .code(EntityFixtures.uniqueCode())
                .path("/menu")
                .build();
        menu.addChild(Menu.builder()
                .parent(menu)
                .name("하위 메뉴")
                .code(EntityFixtures.uniqueCode())
                .path("/menu/child")
                .build());
        Permission permission = authority.addPermission(
                Permission.builder().authority(authority).menu(menu).build());
        permission.addAction(PermissionAction.builder()
                .permission(permission)
                .action(PermissionActionType.READ)
                .build());
        permission.addAction(PermissionAction.builder()
                .permission(permission)
                .action(PermissionActionType.UPDATE)
                .build());

        // DTO로 변환한다.
        AuthorityDto dto = AuthorityDto.from(authority);

        // 권한의 필드와 소유한 하위 DTO가 담겨야 한다.
        assertThat(dto.id()).isEqualTo(authority.getId());
        assertThat(dto.name()).isEqualTo("관리자");
        assertThat(dto.code()).isEqualTo(AuthorityCode.ADMIN);
        assertThat(dto.permissions()).hasSize(1);
        PermissionDto permissionDto = dto.permissions().get(0);
        assertThat(permissionDto.actions())
                .extracting(PermissionActionDto::action)
                .containsExactlyInAnyOrder(PermissionActionType.READ, PermissionActionType.UPDATE);

        // 참조만 하는 메뉴는 하위 컬렉션 없이 담겨야 한다.
        assertThat(permissionDto.menu().name()).isEqualTo("메뉴");
        assertThat(permissionDto.menu().children()).isNull();
    }

    /**
     * 메뉴 DTO가 하위 메뉴는 담고 부모 메뉴는 담지 않는지 확인합니다.
     */
    @Test
    void menuDtoContainsChildrenButNotParent() {
        // 부모 메뉴와 하위 메뉴를 만든다.
        Menu parent = Menu.builder()
                .parent(null)
                .name("부모")
                .code(EntityFixtures.uniqueCode())
                .path("/parent")
                .build();
        parent.addChild(Menu.builder()
                .parent(parent)
                .name("자식")
                .code(EntityFixtures.uniqueCode())
                .path("/parent/child")
                .build());

        // DTO로 변환한다.
        MenuDto dto = MenuDto.from(parent);

        // 하위 메뉴는 담기고 부모는 필드에 없어야 한다.
        assertThat(dto.children()).hasSize(1);
        assertThat(dto.children().get(0).name()).isEqualTo("자식");
        assertThat(dto.children().get(0).children()).isEmpty();
        assertThat(componentNames(MenuDto.class)).doesNotContain("parent", "parentMenuId");
    }

    /**
     * 메뉴를 하위 컬렉션 없이 변환하면 하위 메뉴를 null로 두는지 확인합니다.
     */
    @Test
    void menuFromWithoutChildrenLeavesChildrenNull() {
        // 하위 메뉴가 있는 메뉴를 만든다.
        Menu parent = Menu.builder()
                .parent(null)
                .name("부모")
                .code(EntityFixtures.uniqueCode())
                .path("/parent")
                .build();
        parent.addChild(Menu.builder()
                .parent(parent)
                .name("자식")
                .code(EntityFixtures.uniqueCode())
                .path("/parent/child")
                .build());

        // 하위 컬렉션 없이 변환한다.
        MenuDto dto = MenuDto.fromWithoutChildren(parent);

        // 메뉴의 필드만 담기고 하위 메뉴는 null이어야 한다.
        assertThat(dto.name()).isEqualTo("부모");
        assertThat(dto.code()).isEqualTo(parent.getCode());
        assertThat(dto.path()).isEqualTo("/parent");
        assertThat(dto.children()).isNull();
    }

    /**
     * 하위 DTO는 상위를 포함하지 않는지 확인합니다. (순환 참조 방지)
     */
    @Test
    void childDtosDoNotContainParent() {
        // 권한의 하위 DTO들의 필드 이름을 조회한다.
        assertThat(componentNames(PermissionDto.class)).doesNotContain("authority", "authorityId");
        assertThat(componentNames(PermissionActionDto.class)).doesNotContain("permission", "permissionId");
        assertThat(componentNames(MemberAuthorityDto.class)).doesNotContain("member", "memberId");
    }

    /**
     * 회원 권한 DTO가 권한을 하위 컬렉션 없이 담고 지연 로딩 컬렉션을 건드리지 않는지 확인합니다.
     */
    @Test
    void memberAuthorityDtoContainsAuthorityWithoutChildren() {
        // permission을 가진 권한을 저장하고 회원에게 부여한 뒤 영속성 컨텍스트를 비우고 회원을 조회한다.
        Authority authority =
                Authority.builder().name("관리자").code(AuthorityCode.ADMIN).build();
        Menu menu = Menu.builder()
                .parent(null)
                .name("메뉴")
                .code(EntityFixtures.uniqueCode())
                .path("/menu")
                .build();
        em.persist(menu);
        authority.addPermission(
                Permission.builder().authority(authority).menu(menu).build());
        em.persist(authority);
        Member member = EntityFixtures.member();
        member.addAuthority(
                MemberAuthority.builder().member(member).authority(authority).build());
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 회원을 DTO로 변환한다.
        MemberDto dto = MemberDto.from(loaded);

        // 권한 정보는 담기지만 권한의 하위 컬렉션(permissions)은 null이며 로딩되지 않아야 한다.
        MemberAuthorityDto memberAuthority = dto.authorities().get(0);
        assertThat(memberAuthority.authority().code()).isEqualTo(AuthorityCode.ADMIN);
        assertThat(memberAuthority.authority().permissions()).isNull();
        assertThat(Hibernate.isInitialized(
                        loaded.getAuthorities().get(0).getAuthority().getPermissions()))
                .isFalse();
    }

    /**
     * record의 필드 이름 목록을 반환합니다.
     *
     * @param type record 클래스
     * @return 필드 이름 목록
     */
    private List<String> componentNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
    }
}
