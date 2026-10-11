package dev.choimory.member.domain.authority.command.dto;

import dev.choimory.member.domain.authority.command.entity.Permission;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 권한별 메뉴 접근 DTO입니다. Permission 엔티티와 1:1로 매칭되며 상위(권한)를 포함하지 않습니다.
 * 메뉴는 참조만 하는 관계이므로 하위 메뉴가 null인 메뉴 DTO로 담고, 소유한 액션은 하위 DTO 컬렉션으로 포함합니다.
 *
 * @param id permission ID
 * @param menu 접근할 메뉴 (하위 메뉴 없음)
 * @param actions 허용된 액션
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record PermissionDto(
        String id,
        MenuDto menu,
        List<PermissionActionDto> actions,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * permission 엔티티를 DTO로 변환합니다. 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 합니다.
     *
     * @param permission permission 엔티티
     * @return permission DTO
     */
    public static PermissionDto from(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .menu(MenuDto.fromWithoutChildren(permission.getMenu()))
                .actions(permission.getActions().stream()
                        .map(PermissionActionDto::from)
                        .toList())
                .createdAt(permission.getCreatedAt())
                .modifiedAt(permission.getModifiedAt())
                .deletedAt(permission.getDeletedAt())
                .build();
    }
}
