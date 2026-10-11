package dev.choimory.member.domain.authority.command.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.authority.command.type.PermissionActionType;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * authority 패키지 엔티티(Authority, Menu, Permission, PermissionAction, MemberAuthority) 테스트입니다.
 */
@JpaEntityTest
class AuthorityEntitiesTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    // ---------------------------------------------------------------------------------------------
    // Authority
    // ---------------------------------------------------------------------------------------------

    /**
     * 권한을 생성하고 저장하면 코드가 문자열로 저장되고 같은 값으로 조회되는지 확인합니다.
     */
    @Test
    void authorityIsSavedWithCodeAsString() {
        // 권한을 생성하고 저장한 뒤 조회한다.
        Authority authority =
                Authority.builder().name("관리자").code(AuthorityCode.ADMIN).build();
        assertThat(authority.getPermissions()).isEmpty();
        em.persist(authority);
        em.flush();
        em.clear();
        Authority loaded = em.find(Authority.class, authority.getId());

        // 저장한 값이 그대로 읽혀야 한다.
        assertThat(loaded.getName()).isEqualTo("관리자");
        assertThat(loaded.getCode()).isEqualTo(AuthorityCode.ADMIN);

        // enum은 이름(문자열)으로 저장되어야 한다.
        String code = jdbc.queryForObject("select code from authority where id = ?", String.class, authority.getId());
        assertThat(code).isEqualTo("ADMIN");
    }

    // ---------------------------------------------------------------------------------------------
    // Menu
    // ---------------------------------------------------------------------------------------------

    /**
     * 하위 메뉴가 부모 메뉴에 등록되고 부모와 함께 저장되며 조회 시 부모가 같은지 확인합니다.
     */
    @Test
    void childMenuIsRegisteredAndCascaded() {
        // 부모 메뉴와 하위 메뉴를 만든다.
        Menu parent = Menu.builder()
                .parent(null)
                .name("부모")
                .code(EntityFixtures.uniqueCode())
                .path("/parent")
                .build();
        Menu child = parent.addChild(Menu.builder()
                .parent(parent)
                .name("자식")
                .code(EntityFixtures.uniqueCode())
                .path("/parent/child")
                .build());
        assertThat(parent.getChildren()).containsExactly(child);
        assertThat(parent.getParent()).isNull();

        // 부모만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(parent);
        em.flush();
        em.clear();
        Menu loaded = em.find(Menu.class, parent.getId());

        // 하위 메뉴가 함께 저장되어 조회되고 부모가 같아야 한다.
        assertThat(loaded.getChildren()).hasSize(1);
        Menu loadedChild = loaded.getChildren().get(0);
        assertThat(loadedChild.getName()).isEqualTo("자식");
        assertThat(loadedChild.getParent().getId()).isEqualTo(parent.getId());
    }

    /**
     * 하위 메뉴가 있는 메뉴는 삭제할 수 없는지 확인합니다. (하위 메뉴를 조용히 지우지 않는다)
     */
    @Test
    void menuWithChildrenCannotBeRemoved() {
        // 부모 메뉴와 하위 메뉴를 저장한다.
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
        em.persist(parent);
        em.flush();
        em.clear();

        // 부모 메뉴를 삭제한다.
        em.remove(em.find(Menu.class, parent.getId()));

        // 하위 메뉴가 참조 중이므로 실패해야 한다.
        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // Permission, PermissionAction
    // ---------------------------------------------------------------------------------------------

    /**
     * permission과 action이 권한과 함께 저장되고 조회되며 action이 문자열로 저장되는지 확인합니다.
     */
    @Test
    void permissionAndActionsAreCascadedFromAuthority() {
        // 권한, 메뉴, permission, action 2개를 만든다. (메뉴는 참조만 하므로 따로 저장한다)
        Authority authority =
                Authority.builder().name("관리자").code(AuthorityCode.ADMIN).build();
        Menu menu = Menu.builder()
                .parent(null)
                .name("메뉴")
                .code(EntityFixtures.uniqueCode())
                .path("/menu")
                .build();
        em.persist(menu);
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
        assertThat(authority.getPermissions()).containsExactly(permission);
        assertThat(permission.getActions()).hasSize(2);

        // 권한만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(authority);
        em.flush();
        em.clear();
        Authority loaded = em.find(Authority.class, authority.getId());

        // permission과 action이 함께 저장되어 조회되어야 한다.
        assertThat(loaded.getPermissions()).hasSize(1);
        Permission loadedPermission = loaded.getPermissions().get(0);
        assertThat(loadedPermission.getMenu().getId()).isEqualTo(menu.getId());
        assertThat(loadedPermission.getActions())
                .extracting(PermissionAction::getAction)
                .containsExactlyInAnyOrder(PermissionActionType.READ, PermissionActionType.UPDATE);

        // action은 이름(문자열)으로 저장되어야 한다.
        List<String> actions = jdbc.queryForList("select action from permission_action order by action", String.class);
        assertThat(actions).containsExactly("READ", "UPDATE");
    }

    // ---------------------------------------------------------------------------------------------
    // MemberAuthority
    // ---------------------------------------------------------------------------------------------

    /**
     * 회원 권한이 회원에 등록되고 회원과 함께 저장되며, 회원을 삭제해도 권한은 남는지 확인합니다.
     */
    @Test
    void memberAuthorityIsCascadedFromMemberAndDoesNotRemoveAuthority() {
        // 권한은 따로 저장하고 회원에 권한을 부여한다.
        Authority authority =
                Authority.builder().name("관리자").code(AuthorityCode.ADMIN).build();
        em.persist(authority);
        Member member = EntityFixtures.member();
        MemberAuthority memberAuthority = member.addAuthority(
                MemberAuthority.builder().member(member).authority(authority).build());
        assertThat(member.getAuthorities()).containsExactly(memberAuthority);

        // 회원만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 회원 권한이 함께 저장되어 조회되어야 한다.
        assertThat(loaded.getAuthorities()).hasSize(1);
        assertThat(loaded.getAuthorities().get(0).getAuthority().getCode()).isEqualTo(AuthorityCode.ADMIN);

        // 회원을 삭제하면 회원 권한은 삭제되고 권한은 남아 있어야 한다.
        em.remove(loaded);
        em.flush();
        assertThat(count("member_authority")).isZero();
        assertThat(count("authority")).isEqualTo(1);
    }

    /**
     * 테이블의 행 개수를 조회합니다.
     *
     * @param table 테이블 이름
     * @return 행 개수
     */
    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
