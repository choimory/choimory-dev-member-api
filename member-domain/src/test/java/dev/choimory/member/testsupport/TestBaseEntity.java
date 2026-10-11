package dev.choimory.member.testsupport;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;

/**
 * BaseEntity 동작을 검증하기 위한 테스트 전용 엔티티입니다.
 * 실제 엔티티 스캔(dev.choimory.member.domain)에 섞이지 않도록 별도 패키지에 둡니다.
 */
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
public class TestBaseEntity extends BaseEntity {

    private String name; // 이름

    /**
     * JPA가 사용하는 기본 생성자입니다.
     */
    protected TestBaseEntity() {}

    private TestBaseEntity(String name) {
        this.name = name;
    }

    /**
     * 새 엔티티를 생성합니다.
     *
     * @param name 이름
     * @return 생성된 엔티티
     */
    public static TestBaseEntity create(String name) {
        return new TestBaseEntity(name);
    }

    /**
     * 이름을 변경합니다.
     *
     * @param name 변경할 이름
     */
    public void rename(String name) {
        this.name = name;
    }
}
