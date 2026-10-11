package dev.choimory.member.domain.authority.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

/**
 * 권한별 메뉴 접근 엔티티입니다. (permission 테이블)
 * 권한이 소유하고, 허용된 액션({@link PermissionAction})을 소유합니다. 메뉴는 참조만 합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Permission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "authority_id", nullable = false)
    private Authority authority; // 권한

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu; // 메뉴 (참조만 한다)

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PermissionAction> actions = new ArrayList<>(); // 허용된 액션

    /**
     * 허용된 액션을 등록합니다.
     *
     * @param action 등록할 액션
     * @return 등록된 action
     */
    public PermissionAction addAction(PermissionAction action) {
        this.actions.add(action);
        return action;
    }
}
