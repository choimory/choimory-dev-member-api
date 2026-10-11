package dev.choimory.member.domain.authority.command.entity;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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
 * 메뉴 엔티티입니다. (menu 테이블)
 * 부모 메뉴를 가리키는 자기참조 구조입니다. 하위 메뉴가 있는 메뉴를 삭제하면 하위 메뉴를 조용히 지우지 않고
 * 실패하도록 저장(PERSIST)과 병합(MERGE)만 하위 메뉴에 전파합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Menu extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_menu_id")
    private Menu parent; // 부모 메뉴 (최상위 메뉴는 null)

    @OneToMany(
            mappedBy = "parent",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Menu> children = new ArrayList<>(); // 하위 메뉴

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 메뉴명

    @Column(name = "code", nullable = false, length = 100)
    private String code; // 메뉴 코드

    @Column(name = "path", nullable = false)
    private String path; // 메뉴 경로

    /**
     * 하위 메뉴를 등록합니다.
     *
     * @param child 등록할 하위 메뉴
     * @return 등록된 child
     */
    public Menu addChild(Menu child) {
        this.children.add(child);
        return child;
    }
}
