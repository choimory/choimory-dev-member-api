package dev.choimory.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.authority.command.type.PermissionActionType;
import dev.choimory.member.domain.member.command.type.MemberImageType;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * enum 값 테스트입니다.
 * enum은 DB에 이름(문자열)으로 저장되므로 값의 이름이 바뀌거나 삭제되면 기존 데이터가 깨집니다.
 * 값이 의도치 않게 변경되는 것을 막기 위해 허용 값을 고정합니다. (DDL 주석의 허용 값과 같아야 합니다)
 */
class EnumValuesTest {

    /**
     * MemberStatus의 값이 DB2의 member.status 허용 값과 같은지 확인합니다.
     */
    @Test
    void memberStatusValues() {
        assertThat(names(MemberStatus.values())).containsExactly("REGISTER", "ACTIVATE", "INACTIVATE", "SUSPENDED");
    }

    /**
     * MemberImageType의 값이 DB2의 member_image.type 허용 값과 같은지 확인합니다.
     */
    @Test
    void memberImageTypeValues() {
        assertThat(names(MemberImageType.values())).containsExactly("PROFILE");
    }

    /**
     * AuthorityCode의 값이 DB2의 authority.code 허용 값과 같은지 확인합니다.
     */
    @Test
    void authorityCodeValues() {
        assertThat(names(AuthorityCode.values())).containsExactly("MEMBER", "ADMIN", "MODERATOR");
    }

    /**
     * PermissionActionType의 값이 DB2의 permission_action.action 허용 값과 같은지 확인합니다.
     */
    @Test
    void permissionActionTypeValues() {
        assertThat(names(PermissionActionType.values())).containsExactly("CREATE", "READ", "UPDATE", "DELETE");
    }

    /**
     * enum 값들의 이름 목록을 반환합니다.
     *
     * @param values enum 값 배열
     * @return 이름 목록
     */
    private String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }
}
