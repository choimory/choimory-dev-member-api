package dev.choimory.member.domain.authority.command.dto;

import dev.choimory.member.domain.authority.command.entity.Authority;
import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 권한 DTO입니다. Authority 엔티티와 1:1로 매칭됩니다.
 * 소유한 permission을 하위 DTO 컬렉션으로 포함하며, 조회하지 않은 경우에는 null입니다.
 *
 * @param id 권한 ID
 * @param name 권한명
 * @param code 권한 코드
 * @param permissions 메뉴 접근 정보
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record AuthorityDto(
        String id,
        String name,
        AuthorityCode code,
        List<PermissionDto> permissions,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 권한 엔티티를 하위 DTO를 포함한 DTO로 변환합니다. 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 합니다.
     *
     * @param authority 권한 엔티티
     * @return 권한 DTO
     */
    public static AuthorityDto from(Authority authority) {
        // 권한의 필드를 변환하고 소유한 permission을 하위 DTO로 변환한다.
        return fromWithoutChildren(authority).toBuilder()
                .permissions(authority.getPermissions().stream()
                        .map(PermissionDto::from)
                        .toList())
                .build();
    }

    /**
     * 권한 엔티티를 하위 컬렉션 없이 권한의 필드만 DTO로 변환합니다.
     * permissions는 null이며 엔티티의 지연 로딩 컬렉션을 읽지 않습니다. 참조만 하는 관계(회원 권한)에서 사용합니다.
     *
     * @param authority 권한 엔티티
     * @return 하위 컬렉션이 null인 권한 DTO
     */
    public static AuthorityDto fromWithoutChildren(Authority authority) {
        return AuthorityDto.builder()
                .id(authority.getId())
                .name(authority.getName())
                .code(authority.getCode())
                .createdAt(authority.getCreatedAt())
                .modifiedAt(authority.getModifiedAt())
                .deletedAt(authority.getDeletedAt())
                .build();
    }
}
