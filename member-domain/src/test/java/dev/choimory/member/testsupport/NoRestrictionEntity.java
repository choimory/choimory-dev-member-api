package dev.choimory.member.testsupport;

import dev.choimory.member.domain.common.command.entity.BaseEntity;

/**
 * @SQLRestriction이 없는 BaseEntity 하위 클래스입니다.
 * 엔티티 규칙 테스트(EntityRulesTest)의 검사 로직이 위반을 실제로 잡아내는지 확인하는 데만 사용합니다.
 * @Entity가 아니므로 JPA 스캔 대상이 아닙니다.
 */
public class NoRestrictionEntity extends BaseEntity {}
