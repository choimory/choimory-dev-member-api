package dev.choimory.member.api.common.domain.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * Validation 실패 정보를 표현하는 공통 객체입니다.
 */
@Getter
@Builder(toBuilder = true)
public final class CommonValidateResult {

    private final String field; // 검증 실패 필드명
    private final Object rejectValue; // 거절된 값
    private final String message; // 검증 실패 메시지

    /**
     * Validation 실패 정보를 생성합니다.
     *
     * @param field 검증 실패 필드명
     * @param rejectValue 거절된 값
     * @param message 검증 실패 메시지
     */
    @JsonCreator
    public CommonValidateResult(
            @JsonProperty("field") String field,
            @JsonProperty("rejectValue") Object rejectValue,
            @JsonProperty("message") String message
    ) {
        this.field = field;
        this.rejectValue = rejectValue;
        this.message = message;
    }
}
