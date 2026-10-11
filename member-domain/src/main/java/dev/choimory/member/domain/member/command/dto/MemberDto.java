package dev.choimory.member.domain.member.command.dto;

import dev.choimory.member.domain.agreement.command.dto.MemberAgreementDto;
import dev.choimory.member.domain.authority.command.dto.MemberAuthorityDto;
import dev.choimory.member.domain.member.command.entity.Member;
import dev.choimory.member.domain.member.command.type.MemberStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 회원 DTO입니다. Member 엔티티와 1:1로 매칭됩니다.
 * 소유한 하위 데이터를 하위 DTO 컬렉션으로 포함하며, 하위 컬렉션을 조회하지 않은 경우에는 null입니다.
 *
 * @param id 회원 ID
 * @param email 이메일
 * @param password 암호화된 비밀번호
 * @param nickname 닉네임
 * @param introduce 자기소개
 * @param status 회원 상태
 * @param images 회원 이미지
 * @param suspensions 정지 이력
 * @param authorities 부여된 권한
 * @param agreements 약관 동의 이력
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MemberDto(
        String id,
        String email,
        String password,
        String nickname,
        String introduce,
        MemberStatus status,
        List<MemberImageDto> images,
        List<MemberSuspensionDto> suspensions,
        List<MemberAuthorityDto> authorities,
        List<MemberAgreementDto> agreements,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 회원 엔티티를 하위 DTO를 포함한 DTO로 변환합니다. 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 합니다.
     *
     * @param member 회원 엔티티
     * @return 회원 DTO
     */
    public static MemberDto from(Member member) {
        // 회원의 필드를 변환하고 소유한 하위 컬렉션을 하위 DTO로 변환한다.
        return fromWithoutChildren(member).toBuilder()
                .images(member.getImages().stream().map(MemberImageDto::from).toList())
                .suspensions(member.getSuspensions().stream()
                        .map(MemberSuspensionDto::from)
                        .toList())
                .authorities(member.getAuthorities().stream()
                        .map(MemberAuthorityDto::from)
                        .toList())
                .agreements(member.getAgreements().stream()
                        .map(MemberAgreementDto::from)
                        .toList())
                .build();
    }

    /**
     * 회원 엔티티를 하위 컬렉션 없이 회원의 필드만 DTO로 변환합니다.
     * 하위 컬렉션은 null이며 엔티티의 지연 로딩 컬렉션을 읽지 않습니다. 참조만 하는 관계(팔로우 등)에서 사용합니다.
     *
     * @param member 회원 엔티티
     * @return 하위 컬렉션이 null인 회원 DTO
     */
    public static MemberDto fromWithoutChildren(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .email(member.getEmail())
                .password(member.getPassword())
                .nickname(member.getNickname())
                .introduce(member.getIntroduce())
                .status(member.getStatus())
                .createdAt(member.getCreatedAt())
                .modifiedAt(member.getModifiedAt())
                .deletedAt(member.getDeletedAt())
                .build();
    }
}
