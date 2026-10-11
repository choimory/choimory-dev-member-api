package dev.choimory.member.domain.agreement.command.dto;

import dev.choimory.member.domain.agreement.command.entity.Agreement;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

/**
 * 약관 DTO입니다. Agreement 엔티티와 1:1로 매칭됩니다.
 * 소유한 선택 항목을 하위 DTO 컬렉션으로 포함하며, 조회하지 않은 경우에는 null입니다.
 *
 * @param id 약관 ID
 * @param title 약관 제목
 * @param content 약관 내용
 * @param isMultiple 복수 선택 여부
 * @param answers 선택 항목
 * @param createdAt 생성 일시
 * @param modifiedAt 수정 일시
 * @param deletedAt 삭제 일시
 */
@Builder(toBuilder = true)
public record AgreementDto(
        String id,
        String title,
        String content,
        boolean isMultiple,
        List<AgreementAnswerDto> answers,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        LocalDateTime deletedAt) {

    /**
     * 약관 엔티티를 선택 항목까지 포함한 DTO로 변환합니다. 지연 로딩 컬렉션을 읽으므로 트랜잭션 안에서 호출해야 합니다.
     *
     * @param agreement 약관 엔티티
     * @return 약관 DTO
     */
    public static AgreementDto from(Agreement agreement) {
        return AgreementDto.builder()
                .id(agreement.getId())
                .title(agreement.getTitle())
                .content(agreement.getContent())
                .isMultiple(agreement.isMultiple())
                .answers(agreement.getAnswers().stream()
                        .map(AgreementAnswerDto::from)
                        .toList())
                .createdAt(agreement.getCreatedAt())
                .modifiedAt(agreement.getModifiedAt())
                .deletedAt(agreement.getDeletedAt())
                .build();
    }
}
