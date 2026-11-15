package dev.choimory.member.domain.member.command.entity;

import dev.choimory.member.domain.agreement.command.entity.MemberAgreement;
import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import dev.choimory.member.domain.common.command.entity.BaseEntity;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 회원 엔티티입니다. (member 테이블)
 * 이미지, 정지 이력, 권한, 약관 동의 이력을 소유합니다. 팔로우는 개수가 많이 늘어나므로 컬렉션으로 매핑하지 않고
 * {@link Follow}를 레포지토리에서 페이징 조회합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Member extends BaseEntity {

    @Column(name = "email", nullable = false)
    private String email; // 이메일

    @Column(name = "password", nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(name = "nickname", nullable = false)
    private String nickname; // 닉네임

    @Column(name = "introduce")
    private String introduce; // 자기소개

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberStatus status; // 회원 상태

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberImage> images = new ArrayList<>(); // 회원 이미지

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberSuspension> suspensions = new ArrayList<>(); // 정지 이력

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberAuthority> authorities = new ArrayList<>(); // 부여된 권한

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    @Builder.Default
    private List<MemberAgreement> agreements = new ArrayList<>(); // 약관 동의 이력

    /**
     * 프로필(닉네임, 자기소개)을 변경합니다.
     *
     * @param nickname 변경할 닉네임
     * @param introduce 변경할 자기소개
     */
    public void updateProfile(String nickname, String introduce) {
        this.nickname = nickname;
        this.introduce = introduce;
    }

    /**
     * 비밀번호를 변경합니다.
     *
     * @param password 암호화된 새 비밀번호
     */
    public void changePassword(String password) {
        this.password = password;
    }

    /**
     * 회원 상태를 변경합니다.
     *
     * @param status 변경할 상태
     */
    public void changeStatus(MemberStatus status) {
        this.status = status;
    }

    /**
     * 이미지를 등록합니다.
     *
     * @param image 등록할 이미지
     * @return 등록된 image
     */
    public MemberImage addImage(MemberImage image) {
        this.images.add(image);
        return image;
    }

    /**
     * 정지 이력을 등록합니다.
     *
     * @param suspension 등록할 정지 이력
     * @return 등록된 suspension
     */
    public MemberSuspension addSuspension(MemberSuspension suspension) {
        this.suspensions.add(suspension);
        return suspension;
    }

    /**
     * 권한을 등록합니다.
     *
     * @param authority 등록할 회원 권한
     * @return 등록된 authority
     */
    public MemberAuthority addAuthority(MemberAuthority authority) {
        this.authorities.add(authority);
        return authority;
    }

    /**
     * 약관 동의 이력을 등록합니다.
     *
     * @param agreement 등록할 약관 동의 이력
     * @return 등록된 agreement
     */
    public MemberAgreement addAgreement(MemberAgreement agreement) {
        this.agreements.add(agreement);
        return agreement;
    }
}
