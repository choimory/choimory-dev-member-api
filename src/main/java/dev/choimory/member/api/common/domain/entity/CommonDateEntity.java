package dev.choimory.member.api.common.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * JPA Entity에서 공통으로 사용하는 일시 컬럼 상위 객체입니다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class CommonDateEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt; // 생성 일시

    @LastModifiedDate
    private LocalDateTime modifiedAt; // 수정 일시

    private LocalDateTime deletedAt; // 삭제 일시

    /**
     * 삭제 일시를 기록합니다.
     *
     * @param deletedAt 삭제 일시
     */
    protected void delete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
