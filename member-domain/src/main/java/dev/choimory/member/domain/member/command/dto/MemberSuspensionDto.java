package dev.choimory.member.domain.member.command.dto;

import dev.choimory.member.domain.member.command.entity.MemberSuspension;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 회원 정지 이력 DTO입니다. MemberSuspension 엔티티와 1:1로 매칭되며 상위(회원)를 포함하지 않습니다.
 *
 * @param id 정지 이력 ID
 * @param suspendedAt 정지 시작 일시
 * @param suspendedTo 정지 종료 일시
 * @param reason 정지 사유
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MemberSuspensionDto(
        String id,
        LocalDateTime suspendedAt,
        LocalDateTime suspendedTo,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 정지 이력 엔티티를 DTO로 변환합니다.
     *
     * @param suspension 정지 이력 엔티티
     * @return 정지 이력 DTO
     */
    public static MemberSuspensionDto from(MemberSuspension suspension) {
        return MemberSuspensionDto.builder()
                .id(suspension.getId())
                .suspendedAt(suspension.getSuspendedAt())
                .suspendedTo(suspension.getSuspendedTo())
                .reason(suspension.getReason())
                .createdAt(suspension.getCreatedAt())
                .modifiedAt(suspension.getModifiedAt())
                .deletedAt(suspension.getDeletedAt())
                .build();
    }
}
