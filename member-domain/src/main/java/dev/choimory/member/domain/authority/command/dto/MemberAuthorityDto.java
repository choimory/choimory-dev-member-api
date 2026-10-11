package dev.choimory.member.domain.authority.command.dto;

import dev.choimory.member.domain.authority.command.entity.MemberAuthority;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 회원 권한 DTO입니다. MemberAuthority 엔티티와 1:1로 매칭되며 상위(회원)를 포함하지 않습니다.
 * 권한은 참조만 하는 관계이므로 permissions가 null인 권한 DTO로 담습니다.
 *
 * @param id 회원 권한 ID
 * @param authority 부여된 권한 (하위 컬렉션 없음)
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MemberAuthorityDto(
        String id, AuthorityDto authority, LocalDateTime createdAt, LocalDateTime modifiedAt, LocalDateTime deletedAt) {

    /**
     * 회원 권한 엔티티를 DTO로 변환합니다.
     *
     * @param memberAuthority 회원 권한 엔티티
     * @return 회원 권한 DTO
     */
    public static MemberAuthorityDto from(MemberAuthority memberAuthority) {
        return MemberAuthorityDto.builder()
                .id(memberAuthority.getId())
                .authority(AuthorityDto.fromWithoutChildren(memberAuthority.getAuthority()))
                .createdAt(memberAuthority.getCreatedAt())
                .modifiedAt(memberAuthority.getModifiedAt())
                .deletedAt(memberAuthority.getDeletedAt())
                .build();
    }
}
