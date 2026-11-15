package dev.choimory.member.testsupport;

import dev.choimory.member.domain.common.command.entity.BaseEntity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * @Table이 붙은 BaseEntity 하위 클래스입니다.
 * 엔티티 규칙 테스트(EntityRulesTest)의 검사 로직이 위반을 실제로 잡아내는지 확인하는 데만 사용합니다.
 * @Entity가 아니므로 JPA 스캔 대상이 아닙니다.
 */
@Table(name = "table_annotated_entity")
@SQLRestriction("deleted_at IS NULL")
public class TableAnnotatedEntity extends BaseEntity {}
