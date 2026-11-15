package dev.choimory.member.domain.authority.command.entity;

import dev.choimory.member.domain.authority.command.type.AuthorityCode;
import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 권한 엔티티입니다. (authority 테이블)
 * 메뉴 접근 정보(permission)를 소유합니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Authority extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 권한명

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 50)
    private AuthorityCode code; // 권한 코드

    @OneToMany(mappedBy = "authority", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>(); // 메뉴 접근 정보

    /**
     * 메뉴 접근 정보를 등록합니다.
     *
     * @param permission 등록할 permission
     * @return 등록된 permission
     */
    public Permission addPermission(Permission permission) {
        this.permissions.add(permission);
        return permission;
    }
}
