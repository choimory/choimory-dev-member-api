package dev.choimory.member.domain.agreement.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.Column;
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
 * 약관의 선택 항목 엔티티입니다. (agreement_answer 테이블)
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AgreementAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement; // 약관

    @Column(name = "name", nullable = false)
    private String name; // 선택 항목명

    @Column(name = "is_positive", nullable = false)
    private boolean isPositive; // 긍정적인 답변 여부 (배치와 통계 쿼리용 참고 값)
}
