package dev.choimory.member.domain.common.command.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티의 공통 부모입니다.
 * 공통 컬럼(id, created_at, modified_at, deleted_at)과 아래 동작을 제공합니다.
 * <ul>
 *   <li>id: 서버에서 UUID v7로 채번합니다. 기본 생성자에서 채번하므로 Lombok이 만든 생성자로 만들어도 빠뜨릴 수 없습니다.
 *       JPA가 DB에서 읽을 때도 기본 생성자가 호출되어 채번되지만, 곧바로 DB에 저장된 값으로 덮어씁니다.</li>
 *   <li>Persistable: id를 미리 채번하므로 Spring Data가 새 엔티티인지 판단하지 못해 save 시 불필요한 SELECT가 실행됩니다.
 *       이를 막기 위해 새 엔티티 여부를 직접 알려줍니다.</li>
 *   <li>Auditing: created_at, modified_at을 JPA Auditing으로 채웁니다.</li>
 *   <li>soft delete: {@link #softDelete()}가 deleted_at을 채웁니다. 삭제된 행을 엔티티 쿼리에서 자동으로 제외하려면
 *       각 엔티티가 {@code @SQLRestriction("deleted_at IS NULL")}을 직접 선언해야 합니다. 이 어노테이션은
 *       {@code @MappedSuperclass}인 BaseEntity에 붙이면 하위 엔티티에 상속되지 않습니다. (EntityRulesTest가 검증)
 *       삭제된 행이 필요한 경우(탈퇴철회, hard delete 배치, 가입 중복 확인)는 네이티브 쿼리를 사용합니다.</li>
 * </ul>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id; // 고유 ID (UUID v7)

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt; // 수정 일시

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제 일시 (soft delete)

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true; // 새 엔티티 여부 (저장 또는 조회 후 false)

    /**
     * 엔티티를 생성할 때 UUID v7을 채번하는 기본 생성자입니다.
     * 하위 엔티티의 Lombok 생성자(@NoArgsConstructor, @AllArgsConstructor)가 암묵적으로 호출합니다.
     * JPA가 DB에서 읽을 때도 호출되지만 id는 곧바로 DB에 저장된 값으로 덮어씁니다.
     */
    protected BaseEntity() {
        this.id = newId();
    }

    /**
     * UUID v7을 채번합니다.
     *
     * @return 36자 문자열 UUID v7
     */
    private static String newId() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }

    /**
     * soft delete 처리합니다. deleted_at을 현재 일시로 설정합니다.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 새 엔티티 여부를 반환합니다. true면 Spring Data가 INSERT를 수행하고 false면 merge를 수행합니다.
     *
     * @return 저장 또는 조회 전의 새 엔티티이면 true
     */
    @Override
    public boolean isNew() {
        return newEntity;
    }

    /**
     * 저장하거나 DB에서 읽은 후에는 새 엔티티가 아니도록 표시합니다.
     */
    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
