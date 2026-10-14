package dev.choimory.member.domain.common.command.response;

import lombok.Builder;

/**
 * Validation 실패 정보를 표현하는 공통 객체입니다.
 *
 * @param field 검증 실패 필드명
 * @param rejectValue 거절된 값
 * @param message 검증 실패 메시지
 */
@Builder(toBuilder = true)
public record CommonValidateResult(String field, Object rejectValue, String message) {}
