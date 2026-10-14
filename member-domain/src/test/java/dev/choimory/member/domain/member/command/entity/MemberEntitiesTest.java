package dev.choimory.member.domain.member.command.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.member.command.type.MemberImageType;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * member 패키지 엔티티(Member, MemberImage, MemberSuspension, Follow) 테스트입니다.
 */
@JpaEntityTest
class MemberEntitiesTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    @Autowired
    private JdbcTemplate jdbc; // JDBC 템플릿

    // ---------------------------------------------------------------------------------------------
    // Member
    // ---------------------------------------------------------------------------------------------

    /**
     * 회원 생성 시 필드가 설정되고 UUID v7 id가 채번되며 하위 컬렉션이 비어 있는지 확인합니다.
     */
    @Test
    void createSetsFields() {
        // 회원을 생성한다.
        Member member = Member.builder()
                .email("a@example.com")
                .password("pw")
                .nickname("nick")
                .introduce("intro")
                .status(MemberStatus.REGISTER)
                .build();

        // 전달한 값이 설정되어야 한다.
        assertThat(member.getEmail()).isEqualTo("a@example.com");
        assertThat(member.getPassword()).isEqualTo("pw");
        assertThat(member.getNickname()).isEqualTo("nick");
        assertThat(member.getIntroduce()).isEqualTo("intro");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.REGISTER);

        // id는 UUID v7이고 새 엔티티이며 하위 컬렉션은 비어 있어야 한다.
        assertThat(UUID.fromString(member.getId()).version()).isEqualTo(7);
        assertThat(member.isNew()).isTrue();
        assertThat(member.getImages()).isEmpty();
        assertThat(member.getSuspensions()).isEmpty();
        assertThat(member.getAuthorities()).isEmpty();
        assertThat(member.getAgreements()).isEmpty();
    }

    /**
     * 프로필, 비밀번호, 상태를 변경하는 메소드가 동작하는지 확인합니다.
     */
    @Test
    void updateMethodsChangeFields() {
        // 회원을 생성한다.
        Member member = EntityFixtures.member();

        // 프로필, 비밀번호, 상태를 변경한다.
        member.updateProfile("new-nick", "new-intro");
        member.changePassword("new-pw");
        member.changeStatus(MemberStatus.SUSPENDED);

        // 변경한 값이 반영되어야 한다.
        assertThat(member.getNickname()).isEqualTo("new-nick");
        assertThat(member.getIntroduce()).isEqualTo("new-intro");
        assertThat(member.getPassword()).isEqualTo("new-pw");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    /**
     * 회원을 저장하고 조회하면 같은 값이 읽히고 enum이 문자열로 저장되는지 확인합니다.
     */
    @Test
    void savedMemberIsLoadedWithSameValues() {
        // 회원을 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        Member member = Member.builder()
                .email("a@example.com")
                .password("pw")
                .nickname("nick")
                .introduce("intro")
                .status(MemberStatus.REGISTER)
                .build();
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 저장한 값이 그대로 읽히고 생성 일시만 채워져야 한다.
        assertThat(loaded.getEmail()).isEqualTo("a@example.com");
        assertThat(loaded.getNickname()).isEqualTo("nick");
        assertThat(loaded.getStatus()).isEqualTo(MemberStatus.REGISTER);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getModifiedAt()).isNull();
        assertThat(loaded.isNew()).isFalse();

        // enum은 이름(문자열)으로 저장되어야 한다.
        String status = jdbc.queryForObject("select status from member where id = ?", String.class, member.getId());
        assertThat(status).isEqualTo("REGISTER");
    }

    /**
     * 같은 email의 회원은 저장할 수 없는지 확인합니다.
     */
    @Test
    void duplicateEmailIsRejected() {
        // 같은 email을 가진 회원 두 명을 저장한다.
        Member first = Member.builder()
                .email("same@example.com")
                .password("pw")
                .nickname("nick1")
                .introduce(null)
                .status(MemberStatus.ACTIVATE)
                .build();
        Member second = Member.builder()
                .email("same@example.com")
                .password("pw")
                .nickname("nick2")
                .introduce(null)
                .status(MemberStatus.ACTIVATE)
                .build();
        em.persist(first);
        em.persist(second);

        // unique 제약 위반이어야 한다.
        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
    }

    /**
     * soft delete된 회원은 조회되지 않고 네이티브 쿼리로만 조회되는지 확인합니다.
     */
    @Test
    void softDeletedMemberIsExcluded() {
        // 회원을 저장하고 soft delete한다.
        Member member = EntityFixtures.member();
        em.persist(member);
        member.softDelete();
        em.flush();
        em.clear();

        // 엔티티 조회에서는 제외되어야 한다.
        assertThat(em.find(Member.class, member.getId())).isNull();

        // 네이티브 쿼리로는 조회되어야 한다.
        Integer count = jdbc.queryForObject(
                "select count(*) from member where id = ? and deleted_at is not null", Integer.class, member.getId());
        assertThat(count).isEqualTo(1);
    }

    /**
     * 회원을 삭제하면 소유한 하위 데이터(이미지, 정지 이력)도 함께 삭제되는지 확인합니다.
     */
    @Test
    void removingMemberCascadesToOwnedChildren() {
        // 이미지와 정지 이력을 가진 회원을 저장한다.
        Member member = EntityFixtures.member();
        member.addImage(MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/p")
                .name("n")
                .originFileName("o")
                .size(1L)
                .resizeFilePath("/rp")
                .resizeFileName("rn")
                .resizeFileSize(1L)
                .build());
        member.addSuspension(MemberSuspension.builder()
                .member(member)
                .suspendedAt(Instant.now())
                .suspendedTo(Instant.now().plus(Duration.ofDays(1)))
                .reason("reason")
                .build());
        em.persist(member);
        em.flush();
        em.clear();

        // 회원을 삭제한다.
        em.remove(em.find(Member.class, member.getId()));
        em.flush();

        // 하위 데이터도 함께 삭제되어야 한다.
        assertThat(count("member")).isZero();
        assertThat(count("member_image")).isZero();
        assertThat(count("member_suspension")).isZero();
    }

    // ---------------------------------------------------------------------------------------------
    // MemberImage
    // ---------------------------------------------------------------------------------------------

    /**
     * 이미지가 회원에 등록되고 회원과 함께 저장되며 조회 시 값이 같은지 확인합니다.
     */
    @Test
    void memberImageIsRegisteredAndCascaded() {
        // 이미지를 만들면 회원의 이미지 목록에 등록되어야 한다.
        Member member = EntityFixtures.member();
        MemberImage image = member.addImage(MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/path/orig")
                .name("saved.png")
                .originFileName("origin.png")
                .size(100L)
                .resizeFilePath("/path/r")
                .resizeFileName("r.png")
                .resizeFileSize(10L)
                .build());
        assertThat(member.getImages()).containsExactly(image);
        assertThat(image.getMember()).isSameAs(member);

        // 회원만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 이미지가 함께 저장되어 조회되어야 한다.
        assertThat(loaded.getImages()).hasSize(1);
        MemberImage loadedImage = loaded.getImages().get(0);
        assertThat(loadedImage.getType()).isEqualTo(MemberImageType.PROFILE);
        assertThat(loadedImage.getPath()).isEqualTo("/path/orig");
        assertThat(loadedImage.getName()).isEqualTo("saved.png");
        assertThat(loadedImage.getOriginFileName()).isEqualTo("origin.png");
        assertThat(loadedImage.getSize()).isEqualTo(100L);
        assertThat(loadedImage.getResizeFilePath()).isEqualTo("/path/r");
        assertThat(loadedImage.getResizeFileName()).isEqualTo("r.png");
        assertThat(loadedImage.getResizeFileSize()).isEqualTo(10L);

        // enum은 이름(문자열)으로 저장되어야 한다.
        String type =
                jdbc.queryForObject("select type from member_image where id = ?", String.class, loadedImage.getId());
        assertThat(type).isEqualTo("PROFILE");
    }

    /**
     * soft delete된 이미지는 회원의 이미지 목록에서 제외되는지 확인합니다.
     */
    @Test
    void softDeletedImageIsExcludedFromCollection() {
        // 이미지 2개를 가진 회원을 저장하고 하나를 soft delete한다.
        Member member = EntityFixtures.member();
        member.addImage(MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/p1")
                .name("n1")
                .originFileName("o1")
                .size(1L)
                .resizeFilePath("/rp1")
                .resizeFileName("rn1")
                .resizeFileSize(1L)
                .build());
        MemberImage deleted = member.addImage(MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/p2")
                .name("n2")
                .originFileName("o2")
                .size(1L)
                .resizeFilePath("/rp2")
                .resizeFileName("rn2")
                .resizeFileSize(1L)
                .build());
        em.persist(member);
        deleted.softDelete();
        em.flush();
        em.clear();

        // 삭제되지 않은 이미지만 조회되어야 한다.
        Member loaded = em.find(Member.class, member.getId());
        assertThat(loaded.getImages()).hasSize(1);
    }

    /**
     * Builder로 만든 이미지는 자동 등록되지 않고 addImage()를 호출해야 회원의 이미지 목록에 등록되는지 확인합니다.
     */
    @Test
    void builderDoesNotRegisterToParentUntilAdded() {
        // Builder로 이미지를 만든다.
        Member member = EntityFixtures.member();
        MemberImage image = MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/p")
                .name("n")
                .originFileName("o")
                .size(1L)
                .resizeFilePath("/rp")
                .resizeFileName("rn")
                .resizeFileSize(1L)
                .build();

        // 만들기만 했으므로 목록에 없고, addImage()를 호출하면 등록되어야 한다.
        assertThat(member.getImages()).isEmpty();
        assertThat(member.addImage(image)).isSameAs(image);
        assertThat(member.getImages()).containsExactly(image);
    }

    // ---------------------------------------------------------------------------------------------
    // MemberSuspension
    // ---------------------------------------------------------------------------------------------

    /**
     * 정지 이력이 회원에 등록되고 회원과 함께 저장되며 조회 시 값이 같은지 확인합니다.
     */
    @Test
    void memberSuspensionIsRegisteredAndCascaded() {
        // 정지 이력을 만들면 회원의 정지 이력 목록에 등록되어야 한다.
        Member member = EntityFixtures.member();
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-08T00:00:00Z");
        MemberSuspension suspension = member.addSuspension(MemberSuspension.builder()
                .member(member)
                .suspendedAt(from)
                .suspendedTo(to)
                .reason("규정 위반")
                .build());
        assertThat(member.getSuspensions()).containsExactly(suspension);

        // 회원만 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 정지 이력이 함께 저장되어 조회되어야 한다.
        assertThat(loaded.getSuspensions()).hasSize(1);
        MemberSuspension loadedSuspension = loaded.getSuspensions().get(0);
        assertThat(loadedSuspension.getSuspendedAt()).isEqualTo(from);
        assertThat(loadedSuspension.getSuspendedTo()).isEqualTo(to);
        assertThat(loadedSuspension.getReason()).isEqualTo("규정 위반");
    }

    // ---------------------------------------------------------------------------------------------
    // Follow
    // ---------------------------------------------------------------------------------------------

    /**
     * 자기 자신을 팔로우하면 DB의 CHECK 제약에서 거부되는지 확인합니다. (엔티티는 검증하지 않고 서비스에서 먼저 검증한다)
     */
    @Test
    void selfFollowIsRejectedByDatabaseCheck() {
        // 같은 회원을 follower와 followee로 지정한 팔로우를 저장한다.
        Member member = EntityFixtures.member();
        em.persist(member);

        // flush 시점에 DB가 거부해야 한다.
        assertThatThrownBy(() -> {
                    em.persist(
                            Follow.builder().follower(member).followee(member).build());
                    em.flush();
                })
                .isInstanceOf(PersistenceException.class);
    }

    /**
     * 팔로우를 저장하고 조회하면 follower와 followee가 같은지 확인합니다.
     */
    @Test
    void savedFollowIsLoadedWithMembers() {
        // 회원 둘과 팔로우를 저장한다.
        Member follower = EntityFixtures.member();
        Member followee = EntityFixtures.member();
        em.persist(follower);
        em.persist(followee);
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        em.persist(follow);
        em.flush();
        em.clear();

        // 팔로우를 조회한다.
        Follow loaded = em.find(Follow.class, follow.getId());

        // follower와 followee가 같아야 한다.
        assertThat(loaded.getFollower().getId()).isEqualTo(follower.getId());
        assertThat(loaded.getFollowee().getId()).isEqualTo(followee.getId());
    }

    /**
     * 팔로우는 hard delete되어 행이 삭제되고 회원은 남는지 확인합니다.
     */
    @Test
    void followIsHardDeleted() {
        // 회원 둘과 팔로우를 저장한다.
        Member follower = EntityFixtures.member();
        Member followee = EntityFixtures.member();
        em.persist(follower);
        em.persist(followee);
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        em.persist(follow);
        em.flush();

        // 팔로우를 삭제한다.
        em.remove(follow);
        em.flush();

        // 팔로우 행은 삭제되고 회원은 남아 있어야 한다.
        assertThat(count("follow")).isZero();
        assertThat(count("member")).isEqualTo(2);
    }

    /**
     * 같은 회원 쌍의 팔로우를 중복 저장할 수 없는지 확인합니다.
     */
    @Test
    void duplicateFollowIsRejected() {
        // 회원 둘과 같은 쌍의 팔로우 두 개를 저장한다.
        Member follower = EntityFixtures.member();
        Member followee = EntityFixtures.member();
        em.persist(follower);
        em.persist(followee);
        em.persist(Follow.builder().follower(follower).followee(followee).build());
        em.persist(Follow.builder().follower(follower).followee(followee).build());

        // unique 제약 위반이어야 한다.
        assertThatThrownBy(() -> em.flush()).isInstanceOf(PersistenceException.class);
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
