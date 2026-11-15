package dev.choimory.member.domain.member.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 회원 정지 이력 엔티티입니다. (member_suspension 테이블)
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberSuspension extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 회원

    @Column(name = "suspended_at", nullable = false)
    private Instant suspendedAt; // 정지 시작 일시

    @Column(name = "suspended_to", nullable = false)
    private Instant suspendedTo; // 정지 종료 일시

    @Column(name = "reason", nullable = false, length = 500)
    private String reason; // 정지 사유
}
