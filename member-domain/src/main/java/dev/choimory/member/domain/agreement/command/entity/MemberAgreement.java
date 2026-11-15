package dev.choimory.member.domain.agreement.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import dev.choimory.member.domain.member.command.entity.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 회원이 선택한 약관 항목 엔티티입니다. (member_agreement 테이블)
 * 회원이 소유하며, 선택한 항목(agreement_answer)은 참조만 합니다. 어느 약관인지는 선택 항목에서 알 수 있습니다.
 * 동의 일시는 created_at을 사용하며, 동의를 바꿀 때는 수정하지 않고 기존 행을 soft delete한 뒤 새 행을 만듭니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberAgreement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_answer_id", nullable = false)
    private AgreementAnswer agreementAnswer; // 선택한 약관 항목 (참조만 한다)
}
