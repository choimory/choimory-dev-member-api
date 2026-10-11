package dev.choimory.member.domain.member.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
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
 * 팔로우 엔티티입니다. (follow 테이블)
 * 공통 컬럼 deleted_at은 있지만 사용하지 않으며, 비즈니스 로직에서 hard delete합니다.
 * 개수가 많이 늘어나므로 Member에 컬렉션으로 매핑하지 않고 레포지토리에서 페이징 조회합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Follow extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private Member follower; // 팔로우하는 회원

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followee_id", nullable = false)
    private Member followee; // 팔로우 당하는 회원
}
