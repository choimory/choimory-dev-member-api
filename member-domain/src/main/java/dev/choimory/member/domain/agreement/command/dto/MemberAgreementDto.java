package dev.choimory.member.domain.agreement.command.dto;

import dev.choimory.member.domain.agreement.command.entity.MemberAgreement;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * 회원 약관 동의 이력 DTO입니다. MemberAgreement 엔티티와 1:1로 매칭되며 상위(회원)를 포함하지 않습니다.
 * 선택한 항목은 참조만 하는 관계이며, 어느 약관인지는 담지 않습니다. (필요하면 선택 항목 ID로 약관을 따로 조회한다)
 *
 * @param id 동의 이력 ID
 * @param agreementAnswer 선택한 약관 항목
 * @param createdAt 생성 일시 (동의 일시)
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record MemberAgreementDto(
        String id,
        AgreementAnswerDto agreementAnswer,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 동의 이력 엔티티를 DTO로 변환합니다.
     *
     * @param memberAgreement 동의 이력 엔티티
     * @return 동의 이력 DTO
     */
    public static MemberAgreementDto from(MemberAgreement memberAgreement) {
        return MemberAgreementDto.builder()
                .id(memberAgreement.getId())
                .agreementAnswer(AgreementAnswerDto.from(memberAgreement.getAgreementAnswer()))
                .createdAt(memberAgreement.getCreatedAt())
                .modifiedAt(memberAgreement.getModifiedAt())
                .deletedAt(memberAgreement.getDeletedAt())
                .build();
    }
}
