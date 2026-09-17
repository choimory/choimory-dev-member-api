package dev.choimory.member.api.common.domain.dto;

import lombok.Builder;

/**
 * Validation 실패 정보를 표현하는 공통 객체입니다.
 */
@Builder(toBuilder = true)
public record CommonValidateResult(
		String field, // 검증 실패 필드명
		Object rejectValue, // 거절된 값
		String message // 검증 실패 메시지
) {
}
