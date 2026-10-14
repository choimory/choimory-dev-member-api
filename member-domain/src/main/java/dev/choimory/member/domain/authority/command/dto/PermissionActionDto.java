package dev.choimory.member.domain.authority.command.dto;

import dev.choimory.member.domain.authority.command.entity.PermissionAction;
import dev.choimory.member.domain.authority.command.type.PermissionActionType;
import java.time.Instant;
import lombok.Builder;

/**
 * permission에 허용된 액션 DTO입니다. PermissionAction 엔티티와 1:1로 매칭되며 상위(permission)를 포함하지 않습니다.
 *
 * @param id 액션 ID
 * @param action 허용된 액션
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record PermissionActionDto(
        String id, PermissionActionType action, Instant createdAt, Instant modifiedAt, Instant deletedAt) {

    /**
     * 액션 엔티티를 DTO로 변환합니다.
     *
     * @param permissionAction 액션 엔티티
     * @return 액션 DTO
     */
    public static PermissionActionDto from(PermissionAction permissionAction) {
        return PermissionActionDto.builder()
                .id(permissionAction.getId())
                .action(permissionAction.getAction())
                .createdAt(permissionAction.getCreatedAt())
                .modifiedAt(permissionAction.getModifiedAt())
                .deletedAt(permissionAction.getDeletedAt())
                .build();
    }
}
