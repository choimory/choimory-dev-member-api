package dev.choimory.member.domain.member.command.dto;

import static org.assertj.core.api.Assertions.assertThat;

import dev.choimory.member.domain.EntityFixtures;
import dev.choimory.member.domain.JpaEntityTest;
import dev.choimory.member.domain.agreement.command.entity.Agreement;
import dev.choimory.member.domain.agreement.command.entity.MemberAgreement;
import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.member.command.entity.Follow;
import dev.choimory.member.domain.member.command.entity.Member;
import dev.choimory.member.domain.member.command.entity.MemberImage;
import dev.choimory.member.domain.member.command.entity.MemberSuspension;
import dev.choimory.member.domain.member.command.type.MemberImageType;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import jakarta.persistence.EntityManager;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * member 패키지 DTO(MemberDto, MemberImageDto, MemberSuspensionDto, FollowDto) 테스트입니다.
 */
@JpaEntityTest
class MemberDtosTest {

    @Autowired
    private EntityManager em; // 엔티티 매니저

    /**
     * 회원 DTO가 회원의 필드와 소유한 하위 DTO(이미지, 정지 이력, 권한, 약관 동의 이력)를 모두 담는지 확인합니다.
     */
    @Test
    void memberDtoCopiesOwnFieldsAndOwnedChildren() {
        // 하위 데이터를 모두 가진 회원을 만든다. (DB를 사용하지 않는다)
        Member member = Member.builder()
                .email("a@example.com")
                .password("pw")
                .nickname("nick")
                .introduce("intro")
                .status(MemberStatus.ACTIVATE)
                .build();
        member.addImage(MemberImage.builder()
                .member(member)
                .type(MemberImageType.PROFILE)
                .path("/p")
                .name("n")
                .originFileName("o")
                .size(1L)
                .resizeFilePath("/rp")
                .resizeFileName("rn")
                .resizeFileSize(2L)
                .build());
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-08T00:00:00Z");
        member.addSuspension(MemberSuspension.builder()
                .member(member)
                .suspendedAt(from)
                .suspendedTo(to)
                .reason("reason")
                .build());
        member.addAuthority(MemberAuthority.builder()
                .member(member)
                .authority(Authority.builder()
                        .name("관리자")
                        .code(AuthorityCode.ADMIN)
                        .build())
                .build());
        Agreement agreement = EntityFixtures.agreementWithAnswers();
        member.addAgreement(MemberAgreement.builder()
                .member(member)
                .agreementAnswer(agreement.getAnswers().get(0))
                .build());

        // DTO로 변환한다.
        MemberDto dto = MemberDto.from(member);

        // 회원의 필드가 그대로 담겨야 한다.
        assertThat(dto.id()).isEqualTo(member.getId());
        assertThat(dto.email()).isEqualTo("a@example.com");
        assertThat(dto.password()).isEqualTo("pw");
        assertThat(dto.nickname()).isEqualTo("nick");
        assertThat(dto.introduce()).isEqualTo("intro");
        assertThat(dto.status()).isEqualTo(MemberStatus.ACTIVATE);

        // 소유한 하위 데이터가 하위 DTO로 담겨야 한다.
        assertThat(dto.images()).hasSize(1);
        MemberImageDto image = dto.images().get(0);
        assertThat(image.type()).isEqualTo(MemberImageType.PROFILE);
        assertThat(image.path()).isEqualTo("/p");
        assertThat(image.name()).isEqualTo("n");
        assertThat(image.originFileName()).isEqualTo("o");
        assertThat(image.size()).isEqualTo(1L);
        assertThat(image.resizeFilePath()).isEqualTo("/rp");
        assertThat(image.resizeFileName()).isEqualTo("rn");
        assertThat(image.resizeFileSize()).isEqualTo(2L);
        assertThat(dto.suspensions()).hasSize(1);
        assertThat(dto.suspensions().get(0).suspendedAt()).isEqualTo(from);
        assertThat(dto.suspensions().get(0).suspendedTo()).isEqualTo(to);
        assertThat(dto.suspensions().get(0).reason()).isEqualTo("reason");
        assertThat(dto.authorities()).hasSize(1);
        assertThat(dto.authorities().get(0).authority().code()).isEqualTo(AuthorityCode.ADMIN);
        assertThat(dto.agreements()).hasSize(1);
        assertThat(dto.agreements().get(0).agreementAnswer().name()).isEqualTo("동의");
    }

    /**
     * 저장 후 조회한 회원의 공통 컬럼(생성, 수정, 삭제 일시)이 DTO에 담기는지 확인합니다.
     */
    @Test
    void memberDtoCopiesAuditFields() {
        // 회원을 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        Member member = EntityFixtures.member();
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // DTO로 변환한다.
        MemberDto dto = MemberDto.from(loaded);

        // 엔티티의 공통 컬럼이 그대로 담겨야 한다. (수정 이력이 없으므로 수정 일시와 삭제 일시는 null)
        assertThat(dto.createdAt()).isEqualTo(loaded.getCreatedAt()).isNotNull();
        assertThat(dto.modifiedAt()).isNull();
        assertThat(dto.deletedAt()).isNull();
    }

    /**
     * 하위 DTO는 상위(회원)를 포함하지 않는지 확인합니다. (순환 참조 방지)
     */
    @Test
    void childDtosDoNotContainParent() {
        // 회원의 하위 DTO들의 필드 이름을 조회한다.
        assertThat(componentNames(MemberImageDto.class)).doesNotContain("member", "memberId");
        assertThat(componentNames(MemberSuspensionDto.class)).doesNotContain("member", "memberId");
    }

    /**
     * 컬렉션이 지연 로딩인 회원을 트랜잭션 안에서 변환하면 하위 DTO까지 채워지는지 확인합니다.
     */
    @Test
    void fromConvertsLazyCollectionsInsideTransaction() {
        // 이미지를 가진 회원을 저장하고 영속성 컨텍스트를 비운 뒤 조회한다. (컬렉션은 아직 로딩되지 않음)
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
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());
        assertThat(Hibernate.isInitialized(loaded.getImages())).isFalse();

        // DTO로 변환한다.
        MemberDto dto = MemberDto.from(loaded);

        // 변환 과정에서 컬렉션이 로딩되어 하위 DTO가 채워져야 한다.
        assertThat(dto.images()).hasSize(1);
    }

    /**
     * fromWithoutChildren은 하위 컬렉션을 null로 두고 지연 로딩 컬렉션을 건드리지 않는지 확인합니다.
     */
    @Test
    void fromWithoutChildrenLeavesCollectionsNullAndDoesNotInitializeThem() {
        // 이미지를 가진 회원을 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
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
        em.persist(member);
        em.flush();
        em.clear();
        Member loaded = em.find(Member.class, member.getId());

        // 하위 컬렉션 없이 변환한다.
        MemberDto dto = MemberDto.fromWithoutChildren(loaded);

        // 회원의 필드만 담기고 하위 컬렉션은 null이며, 엔티티의 컬렉션은 로딩되지 않아야 한다.
        assertThat(dto.email()).isEqualTo(loaded.getEmail());
        assertThat(dto.images()).isNull();
        assertThat(dto.suspensions()).isNull();
        assertThat(dto.authorities()).isNull();
        assertThat(dto.agreements()).isNull();
        assertThat(Hibernate.isInitialized(loaded.getImages())).isFalse();
    }

    /**
     * 팔로우 DTO가 follower, followee를 하위 컬렉션 없는 회원 DTO로 담는지 확인합니다.
     */
    @Test
    void followDtoContainsMembersWithoutChildren() {
        // 이미지를 가진 회원 둘과 팔로우를 저장하고 영속성 컨텍스트를 비운 뒤 조회한다.
        Member follower = EntityFixtures.member();
        follower.addImage(MemberImage.builder()
                .member(follower)
                .type(MemberImageType.PROFILE)
                .path("/p")
                .name("n")
                .originFileName("o")
                .size(1L)
                .resizeFilePath("/rp")
                .resizeFileName("rn")
                .resizeFileSize(1L)
                .build());
        Member followee = EntityFixtures.member();
        em.persist(follower);
        em.persist(followee);
        Follow follow = Follow.builder().follower(follower).followee(followee).build();
        em.persist(follow);
        em.flush();
        em.clear();
        Follow loaded = em.find(Follow.class, follow.getId());

        // DTO로 변환한다.
        FollowDto dto = FollowDto.from(loaded);

        // 회원 정보는 담기고 회원의 하위 컬렉션은 null이며 로딩되지 않아야 한다.
        assertThat(dto.id()).isEqualTo(follow.getId());
        assertThat(dto.follower().id()).isEqualTo(follower.getId());
        assertThat(dto.followee().id()).isEqualTo(followee.getId());
        assertThat(dto.follower().images()).isNull();
        assertThat(dto.followee().images()).isNull();
        assertThat(Hibernate.isInitialized(loaded.getFollower().getImages())).isFalse();
    }

    /**
     * toBuilder로 DTO를 복사하며 일부 필드만 바꿀 수 있는지 확인합니다. (DTO는 불변)
     */
    @Test
    void toBuilderCopiesDtoWithChanges() {
        // 회원 DTO를 만든다.
        MemberDto dto = MemberDto.from(EntityFixtures.member());

        // 닉네임만 바꿔 복사한다.
        MemberDto changed = dto.toBuilder().nickname("changed").build();

        // 닉네임만 바뀌고 나머지는 같아야 하며 원본은 그대로여야 한다.
        assertThat(changed.nickname()).isEqualTo("changed");
        assertThat(changed.email()).isEqualTo(dto.email());
        assertThat(dto.nickname()).isNotEqualTo("changed");
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
