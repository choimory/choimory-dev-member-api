package dev.choimory.member.domain.ddl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.choimory.member.domain.MigrationScripts;
import dev.choimory.member.domain.TestcontainersConfiguration;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * docs/flyway의 마이그레이션 SQL(DDL) 테스트입니다.
 * 테이블 구조, 제약조건, 외래키 삭제 동작을 SQL 수준에서 검증합니다.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DdlScriptTest {

    private static final List<String> TABLES = List.of(
            "member",
            "member_image",
            "member_agreement",
            "follow",
            "member_suspension",
            "member_authority",
            "authority",
            "permission",
            "permission_action",
            "menu",
            "agreement",
            "agreement_answer"); // DB2의 전체 테이블

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    @Autowired
    private DataSource dataSource; // 데이터 소스

    // ---------------------------------------------------------------------------------------------
    // 구조
    // ---------------------------------------------------------------------------------------------

    /**
     * DB2의 모든 테이블이 생성되는지 확인합니다.
     */
    @Test
    void allTablesExist() {
        // 현재 스키마의 테이블 이름을 조회한다.
        List<String> names = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()", String.class);

        // DB2의 모든 테이블이 있어야 한다.
        assertThat(names).containsAll(TABLES);
    }

    /**
     * 모든 테이블이 공통 컬럼(id, created_at, modified_at, deleted_at)을 가지는지 확인합니다.
     */
    @Test
    void everyTableHasCommonColumns() {
        for (String table : TABLES) {
            // 공통 컬럼 개수를 조회한다.
            Integer count = jdbc.queryForObject(
                    "select count(*) from information_schema.columns where table_schema = current_schema() and"
                            + " table_name = ? and column_name in ('id', 'created_at', 'modified_at', 'deleted_at')",
                    Integer.class,
                    table);

            // 4개 모두 있어야 한다.
            assertThat(count).as(table).isEqualTo(4);
        }
    }

    /**
     * id와 모든 외래키(_id) 컬럼이 varchar(36)인지 확인합니다.
     */
    @Test
    void idColumnsAreVarchar36() {
        // id, _id로 끝나는 컬럼 중 varchar(36)이 아닌 것을 조회한다.
        List<String> invalid = jdbc.queryForList(
                "select table_name || '.' || column_name from information_schema.columns"
                        + " where table_schema = current_schema() and table_name = any (?)"
                        + " and (column_name = 'id' or column_name like '%\\_id')"
                        + " and not (data_type = 'character varying' and character_maximum_length = 36)",
                String.class, (Object) TABLES.toArray(new String[0]));

        // varchar(36)이 아닌 컬럼이 없어야 한다.
        assertThat(invalid).isEmpty();
    }

    /**
     * enum 컬럼이 DB enum이 아닌 varchar인지 확인합니다.
     */
    @Test
    void enumColumnsAreVarchar() {
        // 서버에서만 enum을 처리하는 컬럼의 타입을 조회한다.
        List<String> types = jdbc.queryForList(
                "select data_type from information_schema.columns where table_schema = current_schema() and ("
                        + "(table_name = 'member' and column_name = 'status')"
                        + " or (table_name = 'member_image' and column_name = 'type')"
                        + " or (table_name = 'authority' and column_name = 'code')"
                        + " or (table_name = 'permission_action' and column_name = 'action'))",
                String.class);

        // 4개 컬럼 모두 varchar여야 한다.
        assertThat(types).hasSize(4).containsOnly("character varying");
    }

    /**
     * agreement 테이블에 type 컬럼이 없는지 확인합니다.
     */
    @Test
    void agreementHasNoTypeColumn() {
        // agreement 테이블의 type 컬럼 개수를 조회한다.
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = current_schema()"
                        + " and table_name = 'agreement' and column_name = 'type'",
                Integer.class);

        // type 컬럼은 제외되었으므로 없어야 한다.
        assertThat(count).isZero();
    }

    /**
     * 모든 시각 컬럼이 timestamptz(timestamp with time zone)인지 확인합니다.
     */
    @Test
    void timeColumnsAreTimestamptz() {
        // 테이블의 시각 컬럼 중 시간대가 없는 timestamp 컬럼을 조회한다.
        List<String> withoutZone = jdbc.queryForList(
                "select table_name || '.' || column_name from information_schema.columns"
                        + " where table_schema = current_schema() and table_name = any (?)"
                        + " and data_type = 'timestamp without time zone'",
                String.class,
                (Object) TABLES.toArray(new String[0]));

        // 시간대가 없는 timestamp 컬럼이 없어야 한다.
        assertThat(withoutZone).isEmpty();

        // 공통 시각 컬럼과 정지 기간 컬럼이 timestamptz여야 한다.
        List<String> withZone = jdbc.queryForList(
                "select table_name || '.' || column_name from information_schema.columns"
                        + " where table_schema = current_schema() and table_name = any (?)"
                        + " and data_type = 'timestamp with time zone'",
                String.class,
                (Object) TABLES.toArray(new String[0]));
        assertThat(withZone).hasSize(38).contains("member.created_at", "member_suspension.suspended_to");
    }

    /**
     * 마이그레이션 SQL을 이미 적용된 DB에 다시 실행해도 오류가 없는지(멱등) 확인합니다.
     * Flyway는 적용된 버전을 다시 실행하지 않지만, V1은 기존 DDL이 적용된 DB에서도 안전하도록 멱등으로 유지합니다.
     */
    @Test
    void ddlIsIdempotent() {
        // 이미 적용된 마이그레이션 SQL을 버전 순으로 두 번 더 실행한다.
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                MigrationScripts.files().stream().map(FileSystemResource::new).toArray(FileSystemResource[]::new));

        // 오류 없이 실행되어야 한다.
        assertThatCode(() -> {
                    populator.execute(dataSource);
                    populator.execute(dataSource);
                })
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------------------------------------------
    // 제약조건
    // ---------------------------------------------------------------------------------------------

    /**
     * member의 email은 soft delete된 행까지 포함해 유일해야 합니다.
     */
    @Test
    void memberEmailIsUnique() {
        // 첫 번째 회원을 저장하고 soft delete 처리한다.
        insertMember("m1", "a@example.com", "nick1");
        jdbc.update("update member set deleted_at = now() where id = 'm1'");

        // soft delete된 회원과 같은 email은 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertMember("m2", "a@example.com", "nick2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * member의 nickname은 유일해야 합니다.
     */
    @Test
    void memberNicknameIsUnique() {
        // 첫 번째 회원을 저장한다.
        insertMember("m1", "a@example.com", "nick");

        // 같은 nickname은 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertMember("m2", "b@example.com", "nick"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 같은 회원이 같은 약관 답변을 중복 선택할 수 없어야 합니다.
     */
    @Test
    void memberAgreementIsUniquePerMemberAndAnswer() {
        // 회원, 약관, 답변, 첫 번째 동의를 저장한다.
        insertMember("m1", "a@example.com", "nick");
        insertAgreement("ag1");
        insertAnswer("an1", "ag1");
        insertMemberAgreement("ma1", "m1", "an1");

        // 같은 회원이 같은 답변을 다시 선택할 수 없어야 한다.
        assertThatThrownBy(() -> insertMemberAgreement("ma2", "m1", "an1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 같은 회원이 같은 권한을 중복으로 가질 수 없어야 합니다.
     */
    @Test
    void memberAuthorityIsUnique() {
        // 회원, 권한, 첫 번째 권한 부여를 저장한다.
        insertMember("m1", "a@example.com", "nick");
        insertAuthority("au1", "MEMBER");
        insertMemberAuthority("mau1", "m1", "au1");

        // 같은 권한을 다시 부여할 수 없어야 한다.
        assertThatThrownBy(() -> insertMemberAuthority("mau2", "m1", "au1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 같은 회원 쌍의 팔로우는 중복될 수 없어야 합니다.
     */
    @Test
    void followIsUnique() {
        // 회원 둘과 첫 번째 팔로우를 저장한다.
        insertMember("m1", "a@example.com", "nick1");
        insertMember("m2", "b@example.com", "nick2");
        insertFollow("f1", "m1", "m2");

        // 같은 팔로우를 다시 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertFollow("f2", "m1", "m2")).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 자기 자신을 팔로우할 수 없어야 합니다.
     */
    @Test
    void followCannotTargetSelf() {
        // 회원을 저장한다.
        insertMember("m1", "a@example.com", "nick");

        // 자기 팔로우는 CHECK 제약으로 막혀야 한다.
        assertThatThrownBy(() -> insertFollow("f1", "m1", "m1")).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * authority의 code는 유일해야 합니다.
     */
    @Test
    void authorityCodeIsUnique() {
        // 첫 번째 권한을 저장한다.
        insertAuthority("au1", "ADMIN");

        // 같은 code는 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertAuthority("au2", "ADMIN")).isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 같은 권한과 메뉴의 permission은 중복될 수 없어야 합니다.
     */
    @Test
    void permissionIsUniquePerAuthorityAndMenu() {
        // 권한, 메뉴, 첫 번째 permission을 저장한다.
        insertAuthority("au1", "ADMIN");
        insertMenu("me1", null, "MENU1");
        insertPermission("p1", "au1", "me1");

        // 같은 조합을 다시 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertPermission("p2", "au1", "me1"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 같은 permission의 같은 action은 중복될 수 없어야 합니다.
     */
    @Test
    void permissionActionIsUniquePerPermissionAndAction() {
        // 권한, 메뉴, permission, 첫 번째 action을 저장한다.
        insertAuthority("au1", "ADMIN");
        insertMenu("me1", null, "MENU1");
        insertPermission("p1", "au1", "me1");
        insertPermissionAction("pa1", "p1", "READ");

        // 같은 action을 다시 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertPermissionAction("pa2", "p1", "READ"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * menu의 code는 유일해야 합니다.
     */
    @Test
    void menuCodeIsUnique() {
        // 첫 번째 메뉴를 저장한다.
        insertMenu("me1", null, "MENU1");

        // 같은 code는 저장할 수 없어야 한다.
        assertThatThrownBy(() -> insertMenu("me2", null, "MENU1")).isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // 외래키 삭제 동작
    // ---------------------------------------------------------------------------------------------

    /**
     * member를 hard delete하면 회원이 소유한 하위 데이터가 함께 삭제되는지 확인합니다.
     */
    @Test
    void deletingMemberCascadesToOwnedRows() {
        // 회원, 다른 회원, 약관/권한과 회원이 소유한 하위 데이터를 저장한다.
        insertMember("m1", "a@example.com", "nick1");
        insertMember("m2", "b@example.com", "nick2");
        insertAuthority("au1", "MEMBER");
        insertAgreement("ag1");
        insertAnswer("an1", "ag1");
        insertMemberImage("img1", "m1");
        insertMemberSuspension("s1", "m1");
        insertMemberAuthority("mau1", "m1", "au1");
        insertMemberAgreement("ma1", "m1", "an1");
        insertFollow("f1", "m1", "m2"); // m1이 follower
        insertFollow("f2", "m2", "m1"); // m1이 followee

        // 회원을 hard delete한다.
        jdbc.update("delete from member where id = 'm1'");

        // 회원이 소유한 하위 데이터가 모두 삭제되어야 한다.
        assertThat(count("member_image")).isZero();
        assertThat(count("member_suspension")).isZero();
        assertThat(count("member_authority")).isZero();
        assertThat(count("member_agreement")).isZero();
        assertThat(count("follow")).isZero();

        // 참조 대상(다른 회원, 권한, 약관, 답변)은 남아 있어야 한다.
        assertThat(count("member")).isEqualTo(1);
        assertThat(count("authority")).isEqualTo(1);
        assertThat(count("agreement")).isEqualTo(1);
        assertThat(count("agreement_answer")).isEqualTo(1);
    }

    /**
     * authority를 삭제하면 소유한 permission과 permission_action이 함께 삭제되는지 확인합니다.
     */
    @Test
    void deletingAuthorityCascadesToPermissions() {
        // 권한, 메뉴, permission, action을 저장한다.
        insertAuthority("au1", "ADMIN");
        insertMenu("me1", null, "MENU1");
        insertPermission("p1", "au1", "me1");
        insertPermissionAction("pa1", "p1", "READ");

        // 권한을 삭제한다.
        jdbc.update("delete from authority where id = 'au1'");

        // permission, permission_action이 삭제되고 메뉴는 남아 있어야 한다.
        assertThat(count("permission")).isZero();
        assertThat(count("permission_action")).isZero();
        assertThat(count("menu")).isEqualTo(1);
    }

    /**
     * agreement를 삭제하면 소유한 agreement_answer가 함께 삭제되는지 확인합니다.
     */
    @Test
    void deletingAgreementCascadesToAnswers() {
        // 약관과 답변을 저장한다.
        insertAgreement("ag1");
        insertAnswer("an1", "ag1");

        // 약관을 삭제한다.
        jdbc.update("delete from agreement where id = 'ag1'");

        // 답변도 삭제되어야 한다.
        assertThat(count("agreement_answer")).isZero();
    }

    /**
     * 회원이 선택한 약관 답변은 삭제할 수 없어야 합니다. (동의 이력 보존)
     */
    @Test
    void deletingReferencedAgreementAnswerIsRestricted() {
        // 회원의 동의 이력을 저장한다.
        insertMember("m1", "a@example.com", "nick");
        insertAgreement("ag1");
        insertAnswer("an1", "ag1");
        insertMemberAgreement("ma1", "m1", "an1");

        // 참조 중인 답변은 삭제할 수 없어야 한다.
        assertThatThrownBy(() -> jdbc.update("delete from agreement_answer where id = 'an1'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 회원에게 부여된 권한은 삭제할 수 없어야 합니다.
     */
    @Test
    void deletingAssignedAuthorityIsRestricted() {
        // 회원에게 권한을 부여한다.
        insertMember("m1", "a@example.com", "nick");
        insertAuthority("au1", "MEMBER");
        insertMemberAuthority("mau1", "m1", "au1");

        // 부여된 권한은 삭제할 수 없어야 한다.
        assertThatThrownBy(() -> jdbc.update("delete from authority where id = 'au1'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * 하위 메뉴가 있는 메뉴는 삭제할 수 없어야 합니다.
     */
    @Test
    void deletingMenuWithChildrenIsRestricted() {
        // 부모 메뉴와 하위 메뉴를 저장한다.
        insertMenu("me1", null, "PARENT");
        insertMenu("me2", "me1", "CHILD");

        // 하위 메뉴가 있는 부모 메뉴는 삭제할 수 없어야 한다.
        assertThatThrownBy(() -> jdbc.update("delete from menu where id = 'me1'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * permission이 참조하는 메뉴는 삭제할 수 없어야 합니다.
     */
    @Test
    void deletingMenuReferencedByPermissionIsRestricted() {
        // 권한, 메뉴, permission을 저장한다.
        insertAuthority("au1", "ADMIN");
        insertMenu("me1", null, "MENU1");
        insertPermission("p1", "au1", "me1");

        // 참조 중인 메뉴는 삭제할 수 없어야 한다.
        assertThatThrownBy(() -> jdbc.update("delete from menu where id = 'me1'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------------------------------------

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private void insertMember(String id, String email, String nickname) {
        jdbc.update(
                "insert into member (id, email, password, nickname, status, created_at)"
                        + " values (?, ?, 'pw', ?, 'ACTIVATE', now())",
                id,
                email,
                nickname);
    }

    private void insertMemberImage(String id, String memberId) {
        jdbc.update(
                "insert into member_image (id, member_id, type, path, name, origin_file_name, size,"
                        + " resize_file_path, resize_file_name, resize_file_size, created_at)"
                        + " values (?, ?, 'PROFILE', '/p', 'n', 'o', 1, '/rp', 'rn', 1, now())",
                id,
                memberId);
    }

    private void insertMemberSuspension(String id, String memberId) {
        jdbc.update(
                "insert into member_suspension (id, member_id, suspended_at, suspended_to, reason, created_at)"
                        + " values (?, ?, now(), now(), 'reason', now())",
                id,
                memberId);
    }

    private void insertAuthority(String id, String code) {
        jdbc.update("insert into authority (id, name, code, created_at) values (?, 'name', ?, now())", id, code);
    }

    private void insertMemberAuthority(String id, String memberId, String authorityId) {
        jdbc.update(
                "insert into member_authority (id, member_id, authority_id, created_at) values (?, ?, ?, now())",
                id,
                memberId,
                authorityId);
    }

    private void insertAgreement(String id) {
        jdbc.update(
                "insert into agreement (id, title, content, is_multiple, created_at)"
                        + " values (?, 'title', 'content', false, now())",
                id);
    }

    private void insertAnswer(String id, String agreementId) {
        jdbc.update(
                "insert into agreement_answer (id, agreement_id, name, is_positive, created_at)"
                        + " values (?, ?, 'name', true, now())",
                id,
                agreementId);
    }

    private void insertMemberAgreement(String id, String memberId, String answerId) {
        jdbc.update(
                "insert into member_agreement (id, member_id, agreement_answer_id, created_at)"
                        + " values (?, ?, ?, now())",
                id,
                memberId,
                answerId);
    }

    private void insertFollow(String id, String followerId, String followeeId) {
        jdbc.update(
                "insert into follow (id, follower_id, followee_id, created_at) values (?, ?, ?, now())",
                id,
                followerId,
                followeeId);
    }

    private void insertMenu(String id, String parentId, String code) {
        jdbc.update(
                "insert into menu (id, parent_menu_id, name, code, path, created_at)"
                        + " values (?, ?, 'name', ?, '/path', now())",
                id,
                parentId,
                code);
    }

    private void insertPermission(String id, String authorityId, String menuId) {
        jdbc.update(
                "insert into permission (id, authority_id, menu_id, created_at) values (?, ?, ?, now())",
                id,
                authorityId,
                menuId);
    }

    private void insertPermissionAction(String id, String permissionId, String action) {
        jdbc.update(
                "insert into permission_action (id, permission_id, action, created_at) values (?, ?, ?, now())",
                id,
                permissionId,
                action);
    }
}
