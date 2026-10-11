package dev.choimory.member.domain.agreement.command.dto;

import dev.choimory.member.domain.agreement.command.entity.AgreementAnswer;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 약관의 선택 항목 DTO입니다. AgreementAnswer 엔티티와 1:1로 매칭되며 상위(약관)를 포함하지 않습니다.
 *
 * @param id 선택 항목 ID
 * @param name 선택 항목명
 * @param isPositive 긍정적인 답변 여부
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record AgreementAnswerDto(
        String id,
        String name,
        boolean isPositive,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 선택 항목 엔티티를 DTO로 변환합니다.
     *
     * @param answer 선택 항목 엔티티
     * @return 선택 항목 DTO
     */
    public static AgreementAnswerDto from(AgreementAnswer answer) {
        return AgreementAnswerDto.builder()
                .id(answer.getId())
                .name(answer.getName())
                .isPositive(answer.isPositive())
                .createdAt(answer.getCreatedAt())
                .modifiedAt(answer.getModifiedAt())
                .deletedAt(answer.getDeletedAt())
                .build();
    }
}
